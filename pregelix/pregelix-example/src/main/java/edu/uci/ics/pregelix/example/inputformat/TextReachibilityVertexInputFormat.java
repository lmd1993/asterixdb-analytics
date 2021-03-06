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
package edu.uci.ics.pregelix.example.inputformat;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.VLongWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import edu.uci.ics.pregelix.api.graph.Vertex;
import edu.uci.ics.pregelix.api.io.VertexReader;
import edu.uci.ics.pregelix.api.io.text.TextVertexInputFormat;
import edu.uci.ics.pregelix.api.io.text.TextVertexInputFormat.TextVertexReader;
import edu.uci.ics.pregelix.api.util.BspUtils;
import edu.uci.ics.pregelix.example.data.VLongWritablePool;

public class TextReachibilityVertexInputFormat extends
        TextVertexInputFormat<VLongWritable, VLongWritable, FloatWritable, VLongWritable> {

    @Override
    public VertexReader<VLongWritable, VLongWritable, FloatWritable, VLongWritable> createVertexReader(
            InputSplit split, TaskAttemptContext context) throws IOException {
        return new TextConnectedComponentsGraphReader(textInputFormat.createRecordReader(split, context));
    }
}

@SuppressWarnings("rawtypes")
class TextReachibilityGraphReader extends TextVertexReader<VLongWritable, VLongWritable, FloatWritable, VLongWritable> {

    private Vertex vertex;
    private VLongWritable vertexId = new VLongWritable();
    private VLongWritablePool pool = new VLongWritablePool();

    public TextReachibilityGraphReader(RecordReader<LongWritable, Text> lineRecordReader) {
        super(lineRecordReader);
    }

    @Override
    public boolean nextVertex() throws IOException, InterruptedException {
        return getRecordReader().nextKeyValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Vertex<VLongWritable, VLongWritable, FloatWritable, VLongWritable> getCurrentVertex() throws IOException,
            InterruptedException {
        pool.reset();
        if (vertex == null) {
            vertex = BspUtils.createVertex(getContext().getConfiguration());
        }
        vertex.getMsgList().clear();
        vertex.getEdges().clear();

        vertex.reset();
        Text line = getRecordReader().getCurrentValue();
        String lineStr = line.toString();
        StringTokenizer tokenizer = new StringTokenizer(lineStr);

        if (tokenizer.hasMoreTokens()) {
            /**
             * set the src vertex id
             */
            long src = Long.parseLong(tokenizer.nextToken());
            vertexId.set(src);
            vertex.setVertexId(vertexId);
            long dest = -1L;

            /**
             * set up edges
             */
            while (tokenizer.hasMoreTokens()) {
                dest = Long.parseLong(tokenizer.nextToken());
                VLongWritable destId = pool.allocate();
                destId.set(dest);
                vertex.addEdge(destId, null);
            }
        }
        return vertex;
    }
}
