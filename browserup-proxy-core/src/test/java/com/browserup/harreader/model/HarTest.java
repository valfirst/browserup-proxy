package com.browserup.harreader.model;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.browserup.harreader.HarReader;
import com.browserup.harreader.HarReaderException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;

import de.sstoehr.harreader.model.HarLog;

public class HarTest extends AbstractMapperTest<Har>{

    @Test
    public void testLogNull() {
        Har har = new Har();
        har.setLog(null);
        Assert.assertNotNull(har.getLog());
    }

    @Override
    public void testMapping() {
        Har har = map("{\"log\": {}}", Har.class);
        Assert.assertNotNull(har.getLog());

        har = map(UNKNOWN_PROPERTY, Har.class);
        Assert.assertNotNull(har);
    }

    @Test
    public void testDeepCopy() throws HarReaderException, IOException {
        Har har = new HarReader().readFromFile(new File("src/test/resources/sstoehr.har"));
        Har deepCopy = har.deepCopy();
        assertEquals(har, deepCopy);
    }

    @Test
    public void testDeepCopyOfEmptyHar() throws IOException {
        Har har = new Har();
        har.setLog(new HarLog());
        Har deepCopy = har.deepCopy();
        assertEquals(har, deepCopy);
    }

    @Test
    public void testAsBytes() throws HarReaderException, IOException {
        File harFile = new File("src/test/resources/sstoehr.har");
        ObjectMapper mapper = new ObjectMapper();
        Har expected = mapper.readValue(harFile, Har.class);

        Har har = new HarReader().readFromFile(harFile);
        assertEquals(mapper.writeValueAsString(expected), new String(har.asBytes(), StandardCharsets.UTF_8));
    }

}
