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

package edu.uci.ics.pregelix.api.converter;

import org.apache.asterix.om.pointables.ARecordVisitablePointable;
import org.apache.asterix.om.types.ARecordType;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import edu.uci.ics.pregelix.api.graph.Vertex;

public interface VertexInputConverter {

    public void open(ARecordType recordType) throws HyracksDataException;

    @SuppressWarnings("rawtypes")
    public void convert(ARecordVisitablePointable recordPointable, Vertex vertex) throws HyracksDataException;

    public void close() throws HyracksDataException;

}
