/*
 * Copyright (C) 2020 Bosch.IO GmbH
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

package org.ossreviewtoolkit.analyzer.managers

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.maps.haveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

import java.io.File

import org.ossreviewtoolkit.downloader.VersionControlSystem
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.Project
import org.ossreviewtoolkit.model.ProjectAnalyzerResult
import org.ossreviewtoolkit.model.Scope
import org.ossreviewtoolkit.model.VcsInfo
import org.ossreviewtoolkit.model.VcsType
import org.ossreviewtoolkit.utils.core.normalizeVcsUrl
import org.ossreviewtoolkit.utils.test.DEFAULT_ANALYZER_CONFIGURATION
import org.ossreviewtoolkit.utils.test.DEFAULT_REPOSITORY_CONFIGURATION
import org.ossreviewtoolkit.utils.test.USER_DIR
import org.ossreviewtoolkit.utils.test.patchExpectedResult

class SpdxDocumentFileFunTest : WordSpec({
    "resolveDependencies()" should {
        "succeed if a project with inline packages is provided" {
            val expectedResult = patchExpectedResult(
                projectDir.parentFile.resolve("spdx-project-xyz-expected-output.yml"),
                url = vcsUrl,
                urlProcessed = normalizeVcsUrl(vcsUrl),
                revision = vcsRevision
            )

            val definitionFile = projectDir.resolve("project-xyz-with-inline-packages.spdx.yml")
            val actualResult = createSpdxDocumentFile().resolveSingleProject(definitionFile).toYaml()

            actualResult shouldBe expectedResult
        }

        "succeed if a project with package references is provided" {
            val expectedResult = patchExpectedResult(
                projectDir.parentFile.resolve("spdx-project-xyz-expected-output.yml"),
                url = vcsUrl,
                urlProcessed = normalizeVcsUrl(vcsUrl),
                revision = vcsRevision
            )

            val definitionFile = projectDir.resolve("project-xyz-with-package-references.spdx.yml")
            val actualResult = createSpdxDocumentFile().resolveSingleProject(definitionFile).toYaml()

            actualResult shouldBe expectedResult
        }

        "succeed if no project is provided" {
            val curlPackageFile = projectDir.resolve("libs/curl/package.spdx.yml")
            val curlId = Identifier("SpdxDocumentFile::curl:7.70.0")

            val opensslPackageFile = projectDir.resolve("libs/openssl/package.spdx.yml")
            val opensslId = Identifier("SpdxDocumentFile:OpenSSL Development Team:openssl:1.1.1g")

            val zlibPackageFile = projectDir.resolve("libs/zlib/package.spdx.yml")
            val zlibId = Identifier("SpdxDocumentFile::zlib:1.2.11")

            val definitionFiles = listOf(curlPackageFile, opensslPackageFile, zlibPackageFile)
            val actualResult = createSpdxDocumentFile().resolveDependencies(definitionFiles, emptyMap())
                // Extract only ProjectAnalyzerResults to avoid depending on other analyzer result specific items (e.g.
                // the dependency graph).
                .projectResults.values.flatten().associateBy { it.project.id }

            actualResult should haveSize(3)

            actualResult[curlId] shouldBe ProjectAnalyzerResult(
                Project(
                    id = curlId,
                    definitionFilePath = curlPackageFile.relativeTo(vcsDir.getRootPath()).invariantSeparatorsPath,
                    authors = sortedSetOf("Daniel Stenberg (daniel@haxx.se)"),
                    declaredLicenses = sortedSetOf("curl"),
                    vcs = VcsInfo(
                        type = VcsType.GIT,
                        url = normalizeVcsUrl(vcsUrl),
                        revision = vcsRevision,
                        path = vcsDir.getPathToRoot(curlPackageFile.parentFile)
                    ),
                    homepageUrl = "https://curl.haxx.se/",
                    scopeDependencies = sortedSetOf(
                        Scope("default")
                    )
                ),
                sortedSetOf()
            )

            actualResult[opensslId] shouldBe ProjectAnalyzerResult(
                Project(
                    id = opensslId,
                    definitionFilePath = opensslPackageFile.relativeTo(vcsDir.getRootPath()).invariantSeparatorsPath,
                    authors = sortedSetOf("OpenSSL Development Team"),
                    declaredLicenses = sortedSetOf("Apache-2.0"),
                    vcs = VcsInfo(
                        type = VcsType.GIT,
                        url = normalizeVcsUrl(vcsUrl),
                        revision = vcsRevision,
                        path = vcsDir.getPathToRoot(opensslPackageFile.parentFile)
                    ),
                    homepageUrl = "https://www.openssl.org/",
                    scopeDependencies = sortedSetOf(
                        Scope("default")
                    )
                ),
                sortedSetOf()
            )

            actualResult[zlibId] shouldBe ProjectAnalyzerResult(
                Project(
                    id = zlibId,
                    definitionFilePath = zlibPackageFile.relativeTo(vcsDir.getRootPath()).invariantSeparatorsPath,
                    authors = sortedSetOf("Jean-loup Gailly", "Mark Adler"),
                    declaredLicenses = sortedSetOf("Zlib"),
                    vcs = VcsInfo(
                        type = VcsType.GIT,
                        url = normalizeVcsUrl(vcsUrl),
                        revision = vcsRevision,
                        path = vcsDir.getPathToRoot(zlibPackageFile.parentFile)
                    ),
                    homepageUrl = "http://zlib.net",
                    scopeDependencies = sortedSetOf(
                        Scope("default")
                    )
                ),
                sortedSetOf()
            )
        }
    }

    "mapDefinitionFiles()" should {
        "remove SPDX documents that do not describe a project if a project file is provided" {
            val projectFile = projectDir.resolve("project-xyz-with-package-references.spdx.yml")
            val packageFile = projectDir.resolve("libs/curl/package.spdx.yml")

            val definitionFiles = listOf(projectFile, packageFile)

            val result = createSpdxDocumentFile().mapDefinitionFiles(definitionFiles)

            result should containExactly(projectFile)
        }

        "keep SPDX documents that do not describe a project if no project file is provided" {
            val packageFileCurl = projectDir.resolve("libs/curl/package.spdx.yml")
            val packageFileZlib = projectDir.resolve("libs/zlib/package.spdx.yml")

            val definitionFiles = listOf(packageFileCurl, packageFileZlib)

            val result = createSpdxDocumentFile().mapDefinitionFiles(definitionFiles)

            result should containExactly(definitionFiles)
        }

        // TODO: Test that we can read in files written by SpdxDocumentReporter.
    }
})

private val projectDir = File("src/funTest/assets/projects/synthetic/spdx").absoluteFile
private val vcsDir = VersionControlSystem.forDirectory(projectDir)!!
private val vcsUrl = vcsDir.getRemoteUrl()
private val vcsRevision = vcsDir.getRevision()

private fun createSpdxDocumentFile() =
    SpdxDocumentFile(
        "SpdxDocumentFile",
        USER_DIR,
        DEFAULT_ANALYZER_CONFIGURATION,
        DEFAULT_REPOSITORY_CONFIGURATION
    )
