package com.browserup.harreader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Objects;

/**
 * Main HTTP Archive Class.
 * @see <a href="http://www.softwareishard.com/blog/har-12-spec/">speicification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Har {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HarLog log;

    /**
     * @return HAR log.
     */
    public HarLog getLog() {
        if (log == null) {
            log = new HarLog();
        }
        return log;
    }

    public void setLog(HarLog log) {
        this.log = log;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Har har = (Har) o;
        return Objects.equals(log, har.log);
    }

    @Override
    public int hashCode() {
        return Objects.hash(log);
    }

    public void writeTo(Writer writer) throws IOException {
        OBJECT_MAPPER.writeValue(writer, this);
    }

    public void writeTo(OutputStream os) throws IOException {
        OBJECT_MAPPER.writeValue(os, this);
    }

    public void writeTo(File file) throws IOException {
        OBJECT_MAPPER.writeValue(file, this);
    }

    /**
     * Serialize HAR as a byte array. It's functionally equivalent to calling {@link #writeTo(OutputStream)} with
     * {@link java.io.ByteArrayOutputStream} and getting bytes, but more efficient. Encoding used will be UTF-8.
     * @return Serialized HAR as a byte array
     * @throws IOException if a low-level I/O problem occurs
     */
    public byte[] asBytes() throws IOException {
        return OBJECT_MAPPER.writeValueAsBytes(this);
    }

    /**
     * <p>Deep copy a {@code Har} using serialization.</p>
     * @return Deep copy of this object
     * @throws IOException if a low-level I/O problem occurs
     */
    public Har deepCopy() throws IOException {
        byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(this);
        return OBJECT_MAPPER.readValue(bytes, Har.class);
    }
}
