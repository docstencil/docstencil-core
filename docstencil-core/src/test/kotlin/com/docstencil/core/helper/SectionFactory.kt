package com.docstencil.core.helper

import kotlin.random.Random

object SectionFactory {
    private val loremIpsumParagraphs = listOf(
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.",
        "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium.",
        "Totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.",
        "Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores.",
        "Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit.",
        "Ut aut reiciendis voluptatibus maiores alias consequatur aut perferendis doloribus asperiores repellat.",
        "At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti.",
        "Et harum quidem rerum facilis est et expedita distinctio nam libero tempore, cum soluta nobis est eligendi optio.",
        "Temporibus autem quibusdam et aut officiis debitis aut rerum necessitatibus saepe eveniet ut et voluptates repudiandae.",
        "Itaque earum rerum hic tenetur a sapiente delectus, ut aut reiciendis voluptatibus maiores alias consequatur.",
        "Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur.",
        "Vel illum qui dolorem eum fugiat quo voluptas nulla pariatur, at vero eos et accusamus et iusto odio dignissimos.",
        "Nam libero tempore, cum soluta nobis est eligendi optio cumque nihil impedit quo minus id quod maxime placeat.",
        "Facere possimus, omnis voluptas assumenda est, omnis dolor repellendus temporibus autem quibusdam et aut officiis.",
        "On the other hand, we denounce with righteous indignation and dislike men who are so beguiled and demoralized.",
        "These cases are perfectly simple and easy to distinguish in a free hour, when our power of choice is untrammelled.",
        "But in certain circumstances and owing to the claims of duty or the obligations of business it will frequently occur.",
    )

    private val topicWords = listOf(
        "Introduction", "Overview", "Analysis", "Summary", "Conclusion",
        "Background", "Methodology", "Results", "Discussion", "Findings",
        "Implementation", "Architecture", "Design", "Development", "Testing",
        "Requirements", "Specification", "Documentation", "Guidelines", "Standards",
        "Performance", "Optimization", "Security", "Scalability", "Reliability",
        "Integration", "Deployment", "Monitoring", "Maintenance", "Support",
    )

    private val loremWords = listOf(
        "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit",
        "sed", "eiusmod", "tempor", "incididunt", "labore", "dolore", "magna",
        "aliqua", "enim", "minim", "veniam", "quis", "nostrud", "exercitation",
    )

    fun createSections(
        depth: Int = 4,
        childrenPerLevel: Int = 5,
        paragraphsPerSection: Int = 5,
        tablesPerSection: Int = 3,
        tableRows: Int = 10,
        tableCols: Int = 4,
        seed: Long = 12345L,
    ): List<com.docstencil.core.helper.Section> {
        val random = Random(seed)
        return generateSectionsAtDepth(
            currentDepth = depth,
            maxDepth = depth,
            childrenPerLevel = childrenPerLevel,
            paragraphsPerSection = paragraphsPerSection,
            tablesPerSection = tablesPerSection,
            tableRows = tableRows,
            tableCols = tableCols,
            random = random,
            indexPath = listOf(),
        )
    }

    private fun generateSectionsAtDepth(
        currentDepth: Int,
        maxDepth: Int,
        childrenPerLevel: Int,
        paragraphsPerSection: Int,
        tablesPerSection: Int,
        tableRows: Int,
        tableCols: Int,
        random: Random,
        indexPath: List<Int>,
    ): List<com.docstencil.core.helper.Section> {
        if (currentDepth < 0) return emptyList()

        return List(childrenPerLevel) { index ->
            val newIndexPath = indexPath + (index + 1)
            val children = if (currentDepth > 0) {
                generateSectionsAtDepth(
                    currentDepth = currentDepth - 1,
                    maxDepth = maxDepth,
                    childrenPerLevel = childrenPerLevel,
                    paragraphsPerSection = paragraphsPerSection,
                    tablesPerSection = tablesPerSection,
                    tableRows = tableRows,
                    tableCols = tableCols,
                    random = random,
                    indexPath = newIndexPath,
                )
            } else {
                emptyList()
            }

            Section(
                title = generateTitle(maxDepth - currentDepth, newIndexPath, random),
                paragraphs = generateParagraphs(paragraphsPerSection, random),
                tables = generateTables(tablesPerSection, tableRows, tableCols, random),
                children = children,
            )
        }
    }

    private fun generateTitle(level: Int, indexPath: List<Int>, random: Random): String {
        val topic = topicWords[random.nextInt(topicWords.size)]
        val path = indexPath.joinToString(".")
        return "Section $path: $topic"
    }

    private fun generateParagraphs(count: Int, random: Random): List<String> {
        return List(count) {
            loremIpsumParagraphs[random.nextInt(loremIpsumParagraphs.size)]
        }
    }

    private fun generateTables(
        tableCount: Int,
        rowsPerTable: Int,
        colsPerTable: Int,
        random: Random,
    ): List<List<List<String>>> {
        val allTables = mutableListOf<List<List<String>>>()

        repeat(tableCount) {
            allTables.add(
                generateTable(
                    rowsPerTable,
                    colsPerTable,
                    random,
                ),
            )
        }

        return allTables
    }

    fun generateTable(
        rowsPerTable: Int,
        colsPerTable: Int,
        random: Random,
    ): List<List<String>> {
        val allRows = mutableListOf<List<String>>()

        repeat(rowsPerTable) {
            val row = mutableListOf<String>()
            repeat(colsPerTable) {
                row.add(loremWords[random.nextInt(loremWords.size)])
            }
            allRows.add(row)
        }

        return allRows
    }
}
