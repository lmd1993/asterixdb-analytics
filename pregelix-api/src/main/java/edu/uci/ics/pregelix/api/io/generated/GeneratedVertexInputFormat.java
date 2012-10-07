/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.uci.ics.pregelix.api.io.generated;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;

import edu.uci.ics.pregelix.api.io.BasicGenInputSplit;
import edu.uci.ics.pregelix.api.io.VertexInputFormat;

/**
 * This VertexInputFormat is meant for testing/debugging. It simply generates
 * some vertex data that can be consumed by test applications.
 */
@SuppressWarnings("rawtypes")
public abstract class GeneratedVertexInputFormat<I extends WritableComparable, V extends Writable, E extends Writable, M extends Writable>
        extends VertexInputFormat<I, V, E, M> {

    @Override
    public List<InputSplit> getSplits(JobContext context, int numWorkers) throws IOException, InterruptedException {
        // This is meaningless, the VertexReader will generate all the test
        // data.
        List<InputSplit> inputSplitList = new ArrayList<InputSplit>();
        for (int i = 0; i < numWorkers; ++i) {
            inputSplitList.add(new BasicGenInputSplit(i, numWorkers));
        }
        return inputSplitList;
    }
}
