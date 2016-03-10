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
package com.wrmsr.search.dsl;

import com.google.common.base.Throwables;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.function.Supplier;

public class FieldSupplierServiceImpl
    implements DocSpecific
{
    private AtomicReaderContext atomicReaderContext;
    private int docId;

    public FieldSupplierServiceImpl()
    {
    }

    @Override
    public void setAtomicReaderContext(AtomicReaderContext atomicReaderContext)
            throws IOException
    {
        this.atomicReaderContext = atomicReaderContext;
    }

    @Override
    public void setDocId(int docId)
    {
        this.docId = docId;
    }

    private Document getDocument()
    {
        try {
            return atomicReaderContext.reader().document(docId);
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public Supplier<String> getStringFieldSupplier(String fieldName) throws IOException
    {
        return () -> getDocument().get(fieldName);
    }

    public Supplier<String[]> getStringsFieldSupplier(String fieldName) throws IOException
    {
        return () -> getDocument().getValues(fieldName);
    }

    public Supplier<BytesRef> getBytesRefFieldSupplier(String fieldName) throws IOException
    {
        return () -> getDocument().getBinaryValue(fieldName);
    }

    public Supplier<BytesRef[]> getBytesRefsFieldSupplier(String fieldName) throws IOException
    {
        return () -> getDocument().getBinaryValues(fieldName);
    }
}
