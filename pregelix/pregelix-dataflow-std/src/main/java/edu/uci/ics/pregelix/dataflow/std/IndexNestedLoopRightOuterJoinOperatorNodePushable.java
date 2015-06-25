/*
 * Copyright 2009-2013 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.pregelix.dataflow.std;

import java.io.DataOutput;
import java.nio.ByteBuffer;

import edu.uci.ics.hyracks.api.comm.IFrame;
import edu.uci.ics.hyracks.api.comm.IFrameTupleAccessor;
import edu.uci.ics.hyracks.api.comm.VSizeFrame;
import edu.uci.ics.hyracks.api.context.IHyracksTaskContext;
import edu.uci.ics.hyracks.api.dataflow.value.IBinaryComparator;
import edu.uci.ics.hyracks.api.dataflow.value.INullWriter;
import edu.uci.ics.hyracks.api.dataflow.value.IRecordDescriptorProvider;
import edu.uci.ics.hyracks.api.dataflow.value.RecordDescriptor;
import edu.uci.ics.hyracks.api.exceptions.HyracksDataException;
import edu.uci.ics.hyracks.dataflow.common.comm.io.ArrayTupleBuilder;
import edu.uci.ics.hyracks.dataflow.common.comm.io.FrameTupleAccessor;
import edu.uci.ics.hyracks.dataflow.common.comm.io.FrameTupleAppender;
import edu.uci.ics.hyracks.dataflow.common.comm.util.FrameUtils;
import edu.uci.ics.hyracks.dataflow.common.data.accessors.ITupleReference;
import edu.uci.ics.hyracks.dataflow.std.base.AbstractUnaryInputUnaryOutputOperatorNodePushable;
import edu.uci.ics.hyracks.storage.am.btree.impls.RangePredicate;
import edu.uci.ics.hyracks.storage.am.common.api.IIndexAccessor;
import edu.uci.ics.hyracks.storage.am.common.api.IIndexCursor;
import edu.uci.ics.hyracks.storage.am.common.api.ITreeIndex;
import edu.uci.ics.hyracks.storage.am.common.dataflow.AbstractTreeIndexOperatorDescriptor;
import edu.uci.ics.hyracks.storage.am.common.dataflow.IndexDataflowHelper;
import edu.uci.ics.hyracks.storage.am.common.impls.NoOpOperationCallback;
import edu.uci.ics.hyracks.storage.am.common.ophelpers.MultiComparator;
import edu.uci.ics.hyracks.storage.am.common.tuples.PermutingFrameTupleReference;

public class IndexNestedLoopRightOuterJoinOperatorNodePushable extends
AbstractUnaryInputUnaryOutputOperatorNodePushable {
    private IndexDataflowHelper treeIndexOpHelper;
    private FrameTupleAccessor accessor;

    private IFrame writeFrame;
    private FrameTupleAppender appender;
    private ArrayTupleBuilder tb;
    private DataOutput dos;

    private ITreeIndex index;
    private boolean isForward;
    private RangePredicate rangePred;
    private MultiComparator lowKeySearchCmp;
    private MultiComparator highKeySearchCmp;
    private IIndexCursor cursor;
    protected IIndexAccessor indexAccessor;

    private RecordDescriptor recDesc;
    private final RecordDescriptor inputRecDesc;

    private PermutingFrameTupleReference lowKey;
    private PermutingFrameTupleReference highKey;

    private INullWriter[] nullWriter;
    private ITupleReference currentTopTuple;
    private boolean match;

    public IndexNestedLoopRightOuterJoinOperatorNodePushable(AbstractTreeIndexOperatorDescriptor opDesc,
            IHyracksTaskContext ctx, int partition, IRecordDescriptorProvider recordDescProvider, boolean isForward,
            int[] lowKeyFields, int[] highKeyFields, INullWriter[] nullWriter) {
        inputRecDesc = recordDescProvider.getInputRecordDescriptor(opDesc.getActivityId(), 0);
        treeIndexOpHelper = (IndexDataflowHelper) opDesc.getIndexDataflowHelperFactory().createIndexDataflowHelper(
                opDesc, ctx, partition);
        this.isForward = isForward;
        this.recDesc = recordDescProvider.getInputRecordDescriptor(opDesc.getActivityId(), 0);

        if (lowKeyFields != null && lowKeyFields.length > 0) {
            lowKey = new PermutingFrameTupleReference();
            lowKey.setFieldPermutation(lowKeyFields);
        }
        if (highKeyFields != null && highKeyFields.length > 0) {
            highKey = new PermutingFrameTupleReference();
            highKey.setFieldPermutation(highKeyFields);
        }
        this.nullWriter = nullWriter;
    }

    protected void setCursor() {
        cursor = indexAccessor.createSearchCursor(false);
    }

    @Override
    public void open() throws HyracksDataException {
        accessor = new FrameTupleAccessor(recDesc);
        try {
            treeIndexOpHelper.open();
            index = (ITreeIndex) treeIndexOpHelper.getIndexInstance();
            writer.open();

            // construct range predicate
            // TODO: Can we construct the multicmps using helper methods?
            int lowKeySearchFields = index.getComparatorFactories().length;
            int highKeySearchFields = index.getComparatorFactories().length;

            IBinaryComparator[] lowKeySearchComparators = new IBinaryComparator[lowKeySearchFields];
            for (int i = 0; i < lowKeySearchFields; i++) {
                lowKeySearchComparators[i] = index.getComparatorFactories()[i].createBinaryComparator();
            }
            lowKeySearchCmp = new MultiComparator(lowKeySearchComparators);

            if (lowKeySearchFields == highKeySearchFields) {
                highKeySearchCmp = lowKeySearchCmp;
            } else {
                IBinaryComparator[] highKeySearchComparators = new IBinaryComparator[highKeySearchFields];
                for (int i = 0; i < highKeySearchFields; i++) {
                    highKeySearchComparators[i] = index.getComparatorFactories()[i].createBinaryComparator();
                }
                highKeySearchCmp = new MultiComparator(highKeySearchComparators);

            }

            rangePred = new RangePredicate(null, null, true, true, lowKeySearchCmp, highKeySearchCmp);
            writeFrame = new VSizeFrame(treeIndexOpHelper.getTaskContext());
            tb = new ArrayTupleBuilder(inputRecDesc.getFields().length + index.getFieldCount());
            dos = tb.getDataOutput();
            appender = new FrameTupleAppender();
            appender.reset(writeFrame, true);

            indexAccessor = index.createAccessor(NoOpOperationCallback.INSTANCE, NoOpOperationCallback.INSTANCE);
            setCursor();

            /** set the search cursor */
            rangePred.setLowKey(null, true);
            rangePred.setHighKey(null, true);
            cursor.reset();
            indexAccessor.search(cursor, rangePred);

            /** set up current top tuple */
            if (cursor.hasNext()) {
                cursor.next();
                currentTopTuple = cursor.getTuple();
                match = false;
            }

        } catch (Exception e) {
            treeIndexOpHelper.close();
            throw new HyracksDataException(e);
        }
    }

    private void writeResults(IFrameTupleAccessor leftAccessor, int tIndex, ITupleReference frameTuple)
            throws Exception {
        tb.reset();
        for (int i = 0; i < inputRecDesc.getFields().length; i++) {
            int tupleStart = leftAccessor.getTupleStartOffset(tIndex);
            int fieldStart = leftAccessor.getFieldStartOffset(tIndex, i);
            int offset = leftAccessor.getFieldSlotsLength() + tupleStart + fieldStart;
            int len = leftAccessor.getFieldEndOffset(tIndex, i) - fieldStart;
            dos.write(leftAccessor.getBuffer().array(), offset, len);
            tb.addFieldEndOffset();
        }
        for (int i = 0; i < frameTuple.getFieldCount(); i++) {
            dos.write(frameTuple.getFieldData(i), frameTuple.getFieldStart(i), frameTuple.getFieldLength(i));
            tb.addFieldEndOffset();
        }
        FrameUtils.appendToWriter(writer, appender, tb.getFieldEndOffsets(), tb.getByteArray(), 0, tb.getSize());
    }

    @Override
    public void nextFrame(ByteBuffer buffer) throws HyracksDataException {
        accessor.reset(buffer);
        int tupleCount = accessor.getTupleCount();
        try {
            for (int i = 0; i < tupleCount && currentTopTuple != null;) {
                if (lowKey != null) {
                    lowKey.reset(accessor, i);
                }
                if (highKey != null) {
                    highKey.reset(accessor, i);
                }
                // TODO: currently use low key only, check what they mean
                int cmp = compare(lowKey, currentTopTuple);
                if ((cmp <= 0 && isForward) || (cmp >= 0 && !isForward)) {
                    if (cmp == 0) {
                        outputMatch(i);
                    }
                    i++;
                } else {
                    moveTreeCursor();
                }
            }
        } catch (Exception e) {
            throw new HyracksDataException(e);
        }
    }

    private void outputMatch(int i) throws Exception {
        writeResults(accessor, i, currentTopTuple);
        match = true;
    }

    private void moveTreeCursor() throws Exception {
        if (!match) {
            writeResults(currentTopTuple);
        }
        if (cursor.hasNext()) {
            cursor.next();
            currentTopTuple = cursor.getTuple();
            match = false;
        } else {
            currentTopTuple = null;
        }
    }

    @Override
    public void close() throws HyracksDataException {
        try {
            while (currentTopTuple != null) {
                moveTreeCursor();
            }

            if (appender.getTupleCount() > 0) {
                FrameUtils.flushFrame(writeFrame.getBuffer(), writer);
            }
            writer.close();
            try {
                cursor.close();
            } catch (Exception e) {
                throw new HyracksDataException(e);
            }
        } catch (Exception e) {
            throw new HyracksDataException(e);
        } finally {
            treeIndexOpHelper.close();
        }
    }

    @Override
    public void fail() throws HyracksDataException {
        try {
            cursor.close();
        } catch (Exception e) {
            throw new HyracksDataException(e);
        } finally {
            treeIndexOpHelper.close();
        }
        writer.fail();
    }

    /** compare tuples */
    private int compare(ITupleReference left, ITupleReference right) throws Exception {
        return lowKeySearchCmp.compare(left, right);
    }

    /** write result for outer case */
    private void writeResults(ITupleReference frameTuple) throws Exception {
        tb.reset();
        for (int i = 0; i < inputRecDesc.getFields().length; i++) {
            nullWriter[i].writeNull(dos);
            tb.addFieldEndOffset();
        }
        for (int i = 0; i < frameTuple.getFieldCount(); i++) {
            dos.write(frameTuple.getFieldData(i), frameTuple.getFieldStart(i), frameTuple.getFieldLength(i));
            tb.addFieldEndOffset();
        }
        FrameUtils.appendToWriter(writer, appender, tb.getFieldEndOffsets(), tb.getByteArray(), 0, tb.getSize());
    }
}