# Compatibility branch instructions

User-approved scope overrides AGENT.md's upstream Java17-only convention: main targets Boot4, main-jdk17 targets Boot3/JDK17, main-jdk8 targets Boot2/JDK8. Build tools may use JDK17; target-VM tests, production bytecode/dependencies and consumer runtime must be verified independently.

New Java classes/tests/helpers use `@author 芋道源码`; preserve upstream authors and licenses. Preserve the official README suffix and add fork usage only as a preface. Do not publish Maven artifacts without explicit authorization; source commit/push is authorized. Only advertise modules that have actually been included and validated.
