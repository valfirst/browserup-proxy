package com.browserup.harreader.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.browserup.harreader.HarReaderMode;

/**
 * @deprecated Use {@link de.sstoehr.harreader.jackson.MapperFactory}
 */
@Deprecated
public interface MapperFactory extends de.sstoehr.harreader.jackson.MapperFactory {

    ObjectMapper instance(HarReaderMode mode);

}
