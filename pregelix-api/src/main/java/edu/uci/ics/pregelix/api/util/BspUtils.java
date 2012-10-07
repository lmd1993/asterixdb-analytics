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

package edu.uci.ics.pregelix.api.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.util.ReflectionUtils;

import edu.uci.ics.pregelix.api.graph.Vertex;
import edu.uci.ics.pregelix.api.graph.VertexCombiner;
import edu.uci.ics.pregelix.api.io.VertexInputFormat;
import edu.uci.ics.pregelix.api.io.VertexOutputFormat;
import edu.uci.ics.pregelix.api.job.PregelixJob;

/**
 * Help to use the configuration to get the appropriate classes or instantiate
 * them.
 */
public class BspUtils {
    private static Configuration defaultConf = null;

    public static void setDefaultConfiguration(Configuration conf) {
        defaultConf = conf;
    }

    /**
     * Get the user's subclassed {@link VertexInputFormat}.
     * 
     * @param conf
     *            Configuration to check
     * @return User's vertex input format class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <I extends WritableComparable, V extends Writable, E extends Writable, M extends Writable> Class<? extends VertexInputFormat<I, V, E, M>> getVertexInputFormatClass(
            Configuration conf) {
        return (Class<? extends VertexInputFormat<I, V, E, M>>) conf.getClass(PregelixJob.VERTEX_INPUT_FORMAT_CLASS,
                null, VertexInputFormat.class);
    }

    /**
     * Create a user vertex input format class
     * 
     * @param conf
     *            Configuration to check
     * @return Instantiated user vertex input format class
     */
    @SuppressWarnings("rawtypes")
    public static <I extends WritableComparable, V extends Writable, E extends Writable, M extends Writable> VertexInputFormat<I, V, E, M> createVertexInputFormat(
            Configuration conf) {
        Class<? extends VertexInputFormat<I, V, E, M>> vertexInputFormatClass = getVertexInputFormatClass(conf);
        VertexInputFormat<I, V, E, M> inputFormat = ReflectionUtils.newInstance(vertexInputFormatClass, conf);
        return inputFormat;
    }

    /**
     * Get the user's subclassed {@link VertexOutputFormat}.
     * 
     * @param conf
     *            Configuration to check
     * @return User's vertex output format class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <I extends WritableComparable, V extends Writable, E extends Writable> Class<? extends VertexOutputFormat<I, V, E>> getVertexOutputFormatClass(
            Configuration conf) {
        return (Class<? extends VertexOutputFormat<I, V, E>>) conf.getClass(PregelixJob.VERTEX_OUTPUT_FORMAT_CLASS,
                null, VertexOutputFormat.class);
    }

    /**
     * Create a user vertex output format class
     * 
     * @param conf
     *            Configuration to check
     * @return Instantiated user vertex output format class
     */
    @SuppressWarnings("rawtypes")
    public static <I extends WritableComparable, V extends Writable, E extends Writable> VertexOutputFormat<I, V, E> createVertexOutputFormat(
            Configuration conf) {
        Class<? extends VertexOutputFormat<I, V, E>> vertexOutputFormatClass = getVertexOutputFormatClass(conf);
        return ReflectionUtils.newInstance(vertexOutputFormatClass, conf);
    }

    /**
     * Get the user's subclassed {@link VertexCombiner}.
     * 
     * @param conf
     *            Configuration to check
     * @return User's vertex combiner class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <I extends WritableComparable, M extends Writable> Class<? extends VertexCombiner<I, M>> getVertexCombinerClass(
            Configuration conf) {
        return (Class<? extends VertexCombiner<I, M>>) conf.getClass(PregelixJob.VERTEX_COMBINER_CLASS, null,
                VertexCombiner.class);
    }

    /**
     * Create a user vertex combiner class
     * 
     * @param conf
     *            Configuration to check
     * @return Instantiated user vertex combiner class
     */
    @SuppressWarnings("rawtypes")
    public static <I extends WritableComparable, M extends Writable> VertexCombiner<I, M> createVertexCombiner(
            Configuration conf) {
        Class<? extends VertexCombiner<I, M>> vertexCombinerClass = getVertexCombinerClass(conf);
        return ReflectionUtils.newInstance(vertexCombinerClass, conf);
    }

    /**
     * Get the user's subclassed Vertex.
     * 
     * @param conf
     *            Configuration to check
     * @return User's vertex class
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <I extends WritableComparable, V extends Writable, E extends Writable, M extends Writable> Class<? extends Vertex<I, V, E, M>> getVertexClass(
            Configuration conf) {
        return (Class<? extends Vertex<I, V, E, M>>) conf.getClass(PregelixJob.VERTEX_CLASS, null, Vertex.class);
    }

    /**
     * Create a user vertex
     * 
     * @param conf
     *            Configuration to check
     * @return Instantiated user vertex
     */
    @SuppressWarnings("rawtypes")
    public static <I extends WritableComparable, V extends Writable, E extends Writable, M extends Writable> Vertex<I, V, E, M> createVertex(
            Configuration conf) {
        Class<? extends Vertex<I, V, E, M>> vertexClass = getVertexClass(conf);
        Vertex<I, V, E, M> vertex = ReflectionUtils.newInstance(vertexClass, conf);
        return vertex;
    }

    /**
     * Get the user's subclassed vertex index class.
     * 
     * @param conf
     *            Configuration to check
     * @return User's vertex index class
     */
    @SuppressWarnings("unchecked")
    public static <I extends Writable> Class<I> getVertexIndexClass(Configuration conf) {
        if (conf == null)
            conf = defaultConf;
        return (Class<I>) conf.getClass(PregelixJob.VERTEX_INDEX_CLASS, WritableComparable.class);
    }

    /**
     * Create a user vertex index
     * 
     * @param conf
     *            Configuration to check
     * @return Instantiated user vertex index
     */
    @SuppressWarnings("rawtypes")
    public static <I extends WritableComparable> I createVertexIndex(Configuration conf) {
        Class<I> vertexClass = getVertexIndexClass(conf);
        try {
            return vertexClass.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("createVertexIndex: Failed to instantiate", e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("createVertexIndex: Illegally accessed", e);
        }
    }

    /**
     * Get the user's subclassed vertex value class.
     * 
     * @param conf
     *            Configuration to check
     * @return User's vertex value class
     */
    @SuppressWarnings("unchecked")
    public static <V extends Writable> Class<V> getVertexValueClass(Configuration conf) {
        return (Class<V>) conf.getClass(PregelixJob.VERTEX_VALUE_CLASS, Writable.class);
    }

    /**
     * Create a user vertex value
     * 
     * @param conf
     *            Configuration to check
     * @return Instantiated user vertex value
     */
    public static <V extends Writable> V createVertexValue(Configuration conf) {
        Class<V> vertexValueClass = getVertexValueClass(conf);
        try {
            return vertexValueClass.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("createVertexValue: Failed to instantiate", e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("createVertexValue: Illegally accessed", e);
        }
    }

    /**
     * Get the user's subclassed edge value class.
     * 
     * @param conf
     *            Configuration to check
     * @return User's vertex edge value class
     */
    @SuppressWarnings("unchecked")
    public static <E extends Writable> Class<E> getEdgeValueClass(Configuration conf) {
        return (Class<E>) conf.getClass(PregelixJob.EDGE_VALUE_CLASS, Writable.class);
    }

    /**
     * Create a user edge value
     * 
     * @param conf
     *            Configuration to check
     * @return Instantiated user edge value
     */
    public static <E extends Writable> E createEdgeValue(Configuration conf) {
        Class<E> edgeValueClass = getEdgeValueClass(conf);
        try {
            return edgeValueClass.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("createEdgeValue: Failed to instantiate", e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("createEdgeValue: Illegally accessed", e);
        }
    }

    /**
     * Get the user's subclassed vertex message value class.
     * 
     * @param conf
     *            Configuration to check
     * @return User's vertex message value class
     */
    @SuppressWarnings("unchecked")
    public static <M extends Writable> Class<M> getMessageValueClass(Configuration conf) {
        if (conf == null)
            conf = defaultConf;
        return (Class<M>) conf.getClass(PregelixJob.MESSAGE_VALUE_CLASS, Writable.class);
    }

    /**
     * Create a user vertex message value
     * 
     * @param conf
     *            Configuration to check
     * @return Instantiated user vertex message value
     */
    public static <M extends Writable> M createMessageValue(Configuration conf) {
        Class<M> messageValueClass = getMessageValueClass(conf);
        try {
            return messageValueClass.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("createMessageValue: Failed to instantiate", e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("createMessageValue: Illegally accessed", e);
        }
    }
}
