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

import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.api.dataflow.IOperatorNodePushable;
import org.apache.hyracks.api.dataflow.value.IBinaryComparatorFactory;
import org.apache.hyracks.api.dataflow.value.IRecordDescriptorProvider;
import org.apache.hyracks.api.dataflow.value.ITypeTraits;
import org.apache.hyracks.api.dataflow.value.RecordDescriptor;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.api.job.JobSpecification;
import org.apache.hyracks.dataflow.std.file.IFileSplitProvider;
import org.apache.hyracks.storage.am.common.api.IIndexLifecycleManagerProvider;
import org.apache.hyracks.storage.am.common.dataflow.AbstractTreeIndexOperatorDescriptor;
import org.apache.hyracks.storage.am.common.dataflow.IIndexDataflowHelperFactory;
import org.apache.hyracks.storage.am.common.impls.NoOpOperationCallbackFactory;
import org.apache.hyracks.storage.common.IStorageManagerInterface;
import org.apache.hyracks.storage.common.file.TransientLocalResourceFactoryProvider;
import edu.uci.ics.pregelix.dataflow.std.base.IRecordDescriptorFactory;
import edu.uci.ics.pregelix.dataflow.std.base.IRuntimeHookFactory;
import edu.uci.ics.pregelix.dataflow.std.base.IUpdateFunctionFactory;

public class TreeSearchFunctionUpdateOperatorDescriptor extends AbstractTreeIndexOperatorDescriptor {

    private static final long serialVersionUID = 1L;

    protected boolean isForward;
    protected int[] lowKeyFields; // fields in input tuple to be used as low
                                  // keys
    protected int[] highKeyFields; // fields in input tuple to be used as high
    // keys
    protected boolean lowKeyInclusive;
    protected boolean highKeyInclusive;

    private final IUpdateFunctionFactory functionFactory;
    private final IRuntimeHookFactory preHookFactory;
    private final IRuntimeHookFactory postHookFactory;
    private final IRecordDescriptorFactory inputRdFactory;

    private final int outputArity;

    public TreeSearchFunctionUpdateOperatorDescriptor(JobSpecification spec, RecordDescriptor recDesc,
            IStorageManagerInterface storageManager, IIndexLifecycleManagerProvider lcManagerProvider,
            IFileSplitProvider fileSplitProvider, ITypeTraits[] typeTraits,
            IBinaryComparatorFactory[] comparatorFactories, boolean isForward, int[] lowKeyFields, int[] highKeyFields,
            boolean lowKeyInclusive, boolean highKeyInclusive, IIndexDataflowHelperFactory dataflowHelperFactory,
            IRecordDescriptorFactory inputRdFactory, int outputArity, IUpdateFunctionFactory functionFactory,
            IRuntimeHookFactory preHookFactory, IRuntimeHookFactory postHookFactory, RecordDescriptor... rDescs) {
        super(spec, 1, outputArity, recDesc, storageManager, lcManagerProvider, fileSplitProvider, typeTraits,
                comparatorFactories, null, dataflowHelperFactory, null, false, false, null,
                TransientLocalResourceFactoryProvider.INSTANCE, NoOpOperationCallbackFactory.INSTANCE,
                NoOpOperationCallbackFactory.INSTANCE);
        this.isForward = isForward;
        this.lowKeyFields = lowKeyFields;
        this.highKeyFields = highKeyFields;
        this.lowKeyInclusive = lowKeyInclusive;
        this.highKeyInclusive = highKeyInclusive;

        this.functionFactory = functionFactory;
        this.preHookFactory = preHookFactory;
        this.postHookFactory = postHookFactory;
        this.inputRdFactory = inputRdFactory;

        for (int i = 0; i < rDescs.length; i++) {
            this.recordDescriptors[i] = rDescs[i];
        }

        this.outputArity = outputArity;
    }

    @Override
    public IOperatorNodePushable createPushRuntime(final IHyracksTaskContext ctx,
            IRecordDescriptorProvider recordDescProvider, int partition, int nPartitions) throws HyracksDataException {
        return new TreeSearchFunctionUpdateOperatorNodePushable(this, ctx, partition, recordDescProvider, isForward,
                lowKeyFields, highKeyFields, lowKeyInclusive, highKeyInclusive, functionFactory, preHookFactory,
                postHookFactory, inputRdFactory, outputArity);
    }
}
