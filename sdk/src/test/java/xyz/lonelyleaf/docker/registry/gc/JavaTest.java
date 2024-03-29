/*
 * Copyright (c) [2019] [lonelyleaf]
 * [docker-registry-gc] is licensed under the Mulan PSL v1.
 * You can use this software according to the terms and conditions of the Mulan PSL v1.
 * You may obtain a copy of Mulan PSL v1 at:
 *    http://license.coscl.org.cn/MulanPSL
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 * PURPOSE.
 *
 * See the Mulan PSL v1 for more details.
 */

package xyz.lonelyleaf.docker.registry.gc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class JavaTest {

    private ObjectMapper mapper = new ObjectMapper()
            .registerModule(new KotlinModule())// better keep those modules
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private DockerRegistryClient client;

    @Before
    public void init() throws IOException {
        //only need properties for test
        final Properties prop = new Properties();
        prop.load(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResourceAsStream("docker-client.properties")));

        client = new DockerRegistryClient(prop.getProperty("baseUrl"),
                prop.getProperty("user"),
                prop.getProperty("pass"),
                mapper);
    }

    @Test
    public void testCatalog() {
        final Catalog catalog = client.catalog();
        Assert.assertFalse(catalog.getRepositories().isEmpty());
        System.out.println(JsonUtil.toPrettyJson(catalog));
    }

}
