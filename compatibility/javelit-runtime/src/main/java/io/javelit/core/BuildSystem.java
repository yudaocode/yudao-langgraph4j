/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
 *
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
package io.javelit.core;

import java.io.IOException;
import java.nio.file.Path;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.VisibleForTesting;

import static io.javelit.core.utils.LangUtils.optional;

@SuppressWarnings("ImmutableEnumChecker")
// ignore the array field in this enum - see https://errorprone.info/bugpattern/ImmutableEnumChecker - this one is internal only - just ensure compileCmdArgs is not mutated
public enum BuildSystem {
  FATJAR_AND_JBANG() {
    @Override
    boolean isUsed() {
      return false;
    }

    @Override
    String obtainClasspath(Path javaFilePath) {
      throw new UnsupportedOperationException("JBang requires the upstream Javelit distribution");
    }
  },
  RUNTIME() {
    @Override
    boolean isUsed() {
      return false;
    }

    @Override
    @Nonnull
    String obtainClasspath(@Nonnull Path javaFilePath) {
      return optional(System.getProperty("java.class.path")).orElse("");
    }
  };

  BuildSystem() {
  }

  abstract boolean isUsed();

  // NOTE: javaFilePath may be required by some build system, even if most won't use it
  @Nonnull
  abstract String obtainClasspath(@Nonnull Path javaFilePath) throws IOException, InterruptedException;

  static BuildSystem inferBuildSystem() {
    for (BuildSystem buildSystem : BuildSystem.values()) {
      if (buildSystem.isUsed()) {
        return buildSystem;
      }
    }

    return BuildSystem.RUNTIME;
  }

  /**
   * Compare two semantic version strings.
   *
   * @param v1 First version string (e.g., "1.0.0" or "1.0.0-SNAPSHOT")
   * @param v2 Second version string (e.g., "0.58.0" or "0.58.0-SNAPSHOT")
   * @return true if v1 is greater than v2, false otherwise
   */
  @VisibleForTesting
  static boolean isVersionGreater(String v1, String v2) {
    // Remove qualifiers like -SNAPSHOT for comparison
    String[] parts1 = v1.split("-")[0].split("\\.");
    String[] parts2 = v2.split("-")[0].split("\\.");

    for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
      int num1 = Integer.parseInt(parts1[i]);
      int num2 = Integer.parseInt(parts2[i]);
      if (num1 > num2) {
        return true;
      }
      if (num1 < num2) {
        return false;
      }
    }

    // If equal up to this point, longer version is greater
    return parts1.length > parts2.length;
  }
}
