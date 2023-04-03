/*
 * Copyright (C) 2017 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.plugins.packagemanagers.stack

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe

import org.ossreviewtoolkit.analyzer.managers.resolveSingleProject
import org.ossreviewtoolkit.model.config.AnalyzerConfiguration
import org.ossreviewtoolkit.model.config.RepositoryConfiguration
import org.ossreviewtoolkit.utils.common.Os
import org.ossreviewtoolkit.utils.test.USER_DIR
import org.ossreviewtoolkit.utils.test.getAssetFile
import org.ossreviewtoolkit.utils.test.patchExpectedResult
import org.ossreviewtoolkit.utils.test.toYaml

class StackFunTest : WordSpec({
    "Resolving project dependencies" should {
        "succeed for quickcheck-state-machine" {
            val definitionFile = getAssetFile("projects/external/quickcheck-state-machine/stack.yaml")
            val suffix = "-windows".takeIf { Os.isWindows }.orEmpty()
            val expectedResultFile = getAssetFile(
                "projects/external/quickcheck-state-machine-expected-output$suffix.yml"
            )

            val result = createStack().resolveSingleProject(definitionFile)

            result.toYaml() shouldBe expectedResultFile.readText()
        }

        "succeed for stack-yesodweb-simple" {
            val definitionFile = getAssetFile("projects/synthetic/stack-yesodweb-simple/stack.yaml")
            val expectedResultFile = getAssetFile("projects/synthetic/stack-yesodweb-simple-expected-output.yml")

            val result = createStack().resolveSingleProject(definitionFile)

            result.toYaml() shouldBe patchExpectedResult(expectedResultFile, definitionFile)
        }
    }
})

private fun createStack() =
    Stack("Stack", USER_DIR, AnalyzerConfiguration(), RepositoryConfiguration())
