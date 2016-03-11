/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wrmsr.search.dsl.field;

import javax.inject.Inject;

import com.google.common.base.Throwables;
import com.wrmsr.search.dsl.DocSpecific;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;

class FieldSupplierServiceImpl
        implements FieldSupplierService, DocSpecific
{

    private final FieldSupplierServiceState state;

    @Inject
    public FieldSupplierServiceImpl(FieldSupplierServiceState fieldSupplierServiceState)
    {
        this.state = fieldSupplierServiceState;
    }

    @Override
    public void setAtomicReaderContext(AtomicReaderContext atomicReaderContext)
            throws IOException
    {
        state.atomicReaderContext = atomicReaderContext;
    }

    @Override
    public void setDocId(int docId)
    {
        state.docId = docId;
    }

    private Document getDocument()
    {
        try {
            return state.atomicReaderContext.reader().document(state.docId);
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public FieldSupplier<String> getStringFieldSupplier(String fieldName)
    {
        return () -> getDocument().get(fieldName);
    }

    @Override
    public FieldSupplier<String[]> getStringsFieldSupplier(String fieldName)
    {
        return () -> getDocument().getValues(fieldName);
    }

    @Override
    public FieldSupplier<BytesRef> getBytesRefFieldSupplier(String fieldName)
    {
        return () -> getDocument().getBinaryValue(fieldName);
    }

    @Override
    public FieldSupplier<BytesRef[]> getBytesRefsFieldSupplier(String fieldName)
    {
        return () -> getDocument().getBinaryValues(fieldName);
    }
}
