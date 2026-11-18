#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.spring.ai.agent.skill;

import org.bsc.langgraph4j.agent.skill.SkillSource;
import org.springframework.core.io.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static java.util.Objects.requireNonNull;

@com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY, getterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE, isGetterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE, setterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
@dev.langchain4j.internal.compat.Java8Record({ "skillRootPath" })
public final class SkillResource extends dev.langchain4j.internal.compat.Java8RecordBase implements SkillSource {

    public static SkillResource of(Resource skillResource) {
        return new SkillResource(skillResource);
    }

    @Override
    public String content() throws IOException {
        final var skillFileResource = skillRootPath.createRelative("SKILL.md");
        try (java.io.InputStream stream = skillFileResource.getInputStream()) {
            return new String(dev.langchain4j.internal.compat.Java8Platform.readAllBytes(stream), StandardCharsets.UTF_8);
        }
    }

    private final Resource skillRootPath;

    public Resource skillRootPath() {
        return this.skillRootPath;
    }

    @com.fasterxml.jackson.annotation.JsonCreator()
    public SkillResource(@com.fasterxml.jackson.annotation.JsonProperty("skillRootPath") Resource skillRootPath) {
        requireNonNull(skillRootPath, "skillRootPath cannot be null!");
        this.skillRootPath = skillRootPath;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        SkillResource that = (SkillResource) other;
        return java.util.Objects.equals(this.skillRootPath, that.skillRootPath);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.skillRootPath);
        return result;
    }

    @Override
    public String toString() {
        return "SkillResource[skillRootPath=" + java.lang.String.valueOf((Object) this.skillRootPath) + "]";
    }
}
