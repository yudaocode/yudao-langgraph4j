package org.bsc.langgraph4j.spring.ai.agent.skill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.UrlResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author 芋道源码
 */
class SkillResourceTest {

    @Test
    void readsUtf8SkillUsingSpringResource(@TempDir Path skillRoot) throws Exception {
        String skill = "# 查询技能\n保留中文指令。\n";
        Files.write(skillRoot.resolve("SKILL.md"), skill.getBytes(StandardCharsets.UTF_8));

        assertEquals(skill, SkillResource.of(new UrlResource(skillRoot.toUri())).content());
    }
}
