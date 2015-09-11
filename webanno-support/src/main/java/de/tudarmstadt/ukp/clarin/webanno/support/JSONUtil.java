/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.support;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.core.JsonGenerator;

import de.tudarmstadt.ukp.clarin.webanno.model.support.spring.ApplicationContextProvider;

public class JSONUtil
{
    /**
     * Convert Java objects into JSON format and write it to a file
     *
     * @param aObject
     *            the object.
     * @param aFile
     *            the file
     * @throws IOException
     *             if an I/O error occurs.
     */
    public static void generateJson(MappingJackson2HttpMessageConverter jsonConverter,
            Object aObject, File aFile)
        throws IOException
    {
        FileUtils.writeStringToFile(aFile, toJsonString(jsonConverter, aObject));
    }

    public static void generateJson(Object aObject, File aFile)
        throws IOException
    {
        FileUtils.writeStringToFile(aFile, toJsonString(aObject));
    }

    public static String toJsonString(MappingJackson2HttpMessageConverter jsonConverter,
            Object aObject)
        throws IOException
    {
        StringWriter out = new StringWriter();

        JsonGenerator jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
                .createJsonGenerator(out);

        jsonGenerator.writeObject(aObject);
        return out.toString();
    }

    public static String toJsonString(Object aObject)
        throws IOException
    {
        return toJsonString(getJsonConverter(), aObject);
    }
    
    public static MappingJackson2HttpMessageConverter getJsonConverter()
    {
        return ApplicationContextProvider.getApplicationContext().getBean("jsonConverter",
                MappingJackson2HttpMessageConverter.class);
    }
}
