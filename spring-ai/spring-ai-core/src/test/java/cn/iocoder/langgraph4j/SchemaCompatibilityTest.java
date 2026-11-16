package cn.iocoder.langgraph4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the producer's intentional Jackson/SLF4J pins through real schema code.
 * @author 芋道源码
 */
class SchemaCompatibilityTest {
    @Test void generatedSchemaValidatesGoodAndBadValuesOnJava8() throws Exception {
        assertEquals("1.8", System.getProperty("java.specification.version"));
        ObjectMapper mapper = new ObjectMapper();
        String generated = JsonSchemaGenerator.generateForType(Reply.class);
        com.networknt.schema.Schema schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema(mapper.readTree(generated));
        assertTrue(schema.validate(mapper.readTree("{\"answer\":\"ok\",\"count\":2}")).isEmpty());
        assertFalse(schema.validate(mapper.readTree("{\"answer\":7,\"count\":\"bad\"}")).isEmpty());
        assertFalse(schema.validate(mapper.readTree("{\"count\":2}")).isEmpty());
    }
    @Test void durationParserUsesJava8PlatformApis() {
        assertEquals("1.8", System.getProperty("java.specification.version"));
        assertEquals(86400, com.ethlo.time.ITU.parseDuration("P1D").getSeconds());
    }
    @Test void structuredOutputConverterUsesThePinnedJacksonRuntime() {
        BeanOutputConverter<Reply> converter = new BeanOutputConverter<>(Reply.class);
        Reply reply = converter.convert("{\"answer\":\"ok\",\"count\":2}");
        assertNotNull(reply);
        assertEquals("ok", reply.answer);
        assertEquals(2, reply.count);
    }
    /**
     * Structured-output fixture.
     *
     * @author 芋道源码
     */
    public static class Reply {
        @JsonProperty(required = true) public String answer;
        public int count;
    }
}
