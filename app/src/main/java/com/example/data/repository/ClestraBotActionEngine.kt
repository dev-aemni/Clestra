package com.example.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class BotActionDirective(
    val action: String = "",
    val peer: String? = null,
    val text: String? = null,
    val name: String? = null,
    val code: String? = null,
    val tag: String? = null,
    val minutes: Long? = null,
    val online: Boolean? = null
)

data class ParsedBotResponse(
    val visibleReply: String,
    val actions: List<BotActionDirective>
)

/**
 * Parses LLM [[CLESTRA_ACTION:{...}]] directives as well as natural-language offline commands
 * so Clestra can send, read, write/edit, delete, clear, block/unblock, mute/unmute, pin,
 * add contacts, update profile/tags, and sync relay seamlessly.
 */
object ClestraBotActionEngine {

    private val actionBlockRegex = Regex("""\[\[CLESTRA_ACTION:(\{.*?\})\]\]""")
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val directiveAdapter = moshi.adapter(BotActionDirective::class.java)

    fun parseModelResponse(rawReply: String): ParsedBotResponse {
        val actions = mutableListOf<BotActionDirective>()
        actionBlockRegex.findAll(rawReply).forEach { match ->
            val jsonStr = match.groupValues[1]
            parseDirectiveJson(jsonStr)?.let { actions.add(it) }
        }
        val cleanText = actionBlockRegex.replace(rawReply, "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
        return ParsedBotResponse(
            visibleReply = cleanText,
            actions = actions
        )
    }

    internal fun parseDirectiveJson(jsonStr: String): BotActionDirective? {
        return try {
            val raw = directiveAdapter.fromJson(jsonStr) ?: return null
            val action = raw.action.trim().lowercase()
            if (action.isEmpty()) return null
            raw.copy(
                action = action,
                peer = raw.peer?.trim()?.ifBlank { null },
                text = raw.text?.trim()?.ifBlank { null },
                name = raw.name?.trim()?.ifBlank { null },
                code = raw.code?.trim()?.ifBlank { null },
                tag = raw.tag?.trim(),
                minutes = raw.minutes?.takeIf { it > 0L }
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Natural-language command parser used when operating offline or without an external AI key.
     * Returns null when the prompt is general conversation rather than a concrete command.
     */
    fun parseOfflineCommand(userMessage: String): Pair<String, List<BotActionDirective>>? {
        val raw = userMessage.trim()
        if (raw.isEmpty()) return null

        // 1. Add contact: "add contact CLS-XXXX [as Name]"
        Regex("""^(?:add|create)\s+(?:contact|peer|chat)\s+(CLS-[A-Za-z0-9-]+)(?:\s+(?:as|named|name)\s+(.+))?$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)?.let { m ->
                val id = m.groupValues[1].uppercase()
                val name = m.groupValues[2].trim().ifBlank { null }
                return "Adding contact **$id** now." to listOf(
                    BotActionDirective(action = "add_contact", peer = id, name = name)
                )
            }

        // 2. Send message: "send <text> to <peer>" or "message <peer>: <text>"
        Regex("""^send\s+(?:message\s+)?["']?(.+?)["']?\s+to\s+([A-Za-z0-9_.\- ]+)$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)?.let { m ->
                val msg = m.groupValues[1].trim()
                val target = m.groupValues[2].trim()
                return "Sending your encrypted message to **$target**." to listOf(
                    BotActionDirective(action = "send_message", peer = target, text = msg)
                )
            }
        Regex("""^(?:message|tell|dm)\s+(CLS-[A-Za-z0-9-]+|[A-Za-z0-9_.-]+)\s*(?::|-|that)?\s+(.+)$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)?.let { m ->
                val target = m.groupValues[1].trim()
                val msg = m.groupValues[2].trim()
                return "Sending your message to **$target**." to listOf(
                    BotActionDirective(action = "send_message", peer = target, text = msg)
                )
            }

        // 3. Edit last message: "edit last message [in/with <peer>] to <new text>"
        Regex("""^edit\s+(?:last\s+)?message\s+(?:(?:to|for|with|in)\s+([A-Za-z0-9_.-]+)\s+)?(?:to|as)\s+(.+)$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)?.let { m ->
                val target = m.groupValues[1].trim().ifBlank { null }
                val newText = m.groupValues[2].trim()
                return "Updating the latest message." to listOf(
                    BotActionDirective(action = "edit_last_message", peer = target, text = newText)
                )
            }

        // 4. Delete last message
        Regex("""^(?:delete|remove)\s+last\s+message(?:\s+(?:in|with|for|from)\s+(.+))?$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)?.let { m ->
                val target = m.groupValues[1].trim().ifBlank { null }
                return "Deleting the last message." to listOf(
                    BotActionDirective(action = "delete_last_message", peer = target)
                )
            }

        // 5. Clear chat history
        Regex("""^clear\s+(?:chat|history|messages)(?:\s+(?:with|in|for)\s+(.+))?$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)?.let { m ->
                val target = m.groupValues[1].trim().ifBlank { null }
                return "Clearing conversation history." to listOf(
                    BotActionDirective(action = "clear_chat", peer = target)
                )
            }

        // 6. Delete chat / contact
        Regex("""^(?:delete|remove)\s+(?:chat|conversation|contact)\s+(?:with\s+)?(.+)$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)?.let { m ->
                val target = m.groupValues[1].trim()
                return "Removing chat with **$target**." to listOf(
                    BotActionDirective(action = "delete_chat", peer = target)
                )
            }

        // 7. Block / Unblock peer
        Regex("""^unblock\s+(.+)$""", RegexOption.IGNORE_CASE).matchEntire(raw)?.let { m ->
            val target = m.groupValues[1].trim()
            return "Unblocking **$target**." to listOf(
                BotActionDirective(action = "unblock_peer", peer = target)
            )
        }
        Regex("""^block\s+(.+)$""", RegexOption.IGNORE_CASE).matchEntire(raw)?.let { m ->
            val target = m.groupValues[1].trim()
            return "Blocking **$target**." to listOf(
                BotActionDirective(action = "block_peer", peer = target)
            )
        }

        // 8. Mute / Unmute peer
        Regex("""^unmute\s+(.+)$""", RegexOption.IGNORE_CASE).matchEntire(raw)?.let { m ->
            val target = m.groupValues[1].trim()
            return "Restoring notifications for **$target**." to listOf(
                BotActionDirective(action = "unmute_peer", peer = target)
            )
        }
        Regex("""^mute\s+(.+?)(?:\s+for\s+(\d+)\s*(h|hr|hrs|hour|hours|m|min|mins|minutes?))?$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)?.let { m ->
                val target = m.groupValues[1].trim()
                val amount = m.groupValues[2].toLongOrNull()
                val unit = m.groupValues[3].lowercase()
                val minutes = when {
                    amount == null -> null
                    unit.startsWith("h") -> amount * 60L
                    else -> amount
                }
                return "Muting **$target**." to listOf(
                    BotActionDirective(action = "mute_peer", peer = target, minutes = minutes)
                )
            }

        // 9. Pin / Unpin
        Regex("""^unpin\s+(.+)$""", RegexOption.IGNORE_CASE).matchEntire(raw)?.let { m ->
            val target = m.groupValues[1].trim()
            return "Unpinning **$target**." to listOf(
                BotActionDirective(action = "unpin_chat", peer = target)
            )
        }
        Regex("""^pin\s+(.+)$""", RegexOption.IGNORE_CASE).matchEntire(raw)?.let { m ->
            val target = m.groupValues[1].trim()
            return "Pinning **$target**." to listOf(
                BotActionDirective(action = "pin_chat", peer = target)
            )
        }

        // 10. Toggle online/relay mode
        Regex("""^(?:set|switch)\s+(.+?)\s+(online|offline|p2p|relay)$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)?.let { m ->
                val target = m.groupValues[1].trim()
                val mode = m.groupValues[2].lowercase()
                val isOnline = mode == "online" || mode == "p2p"
                return "Updating route mode for **$target**." to listOf(
                    BotActionDirective(action = "set_peer_online", peer = target, online = isOnline)
                )
            }

        // 11. Update profile bio / name
        Regex("""^(?:set|update|change)\s+(?:my\s+)?bio\s+(?:to\s+)?(.+)$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)?.let { m ->
                val bio = m.groupValues[1].trim()
                return "Updating your profile bio." to listOf(
                    BotActionDirective(action = "update_bio", text = bio)
                )
            }
        Regex("""^(?:set|update|change)\s+(?:my\s+)?(?:display\s+)?name\s+(?:to\s+)?(.+)$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)?.let { m ->
                val newName = m.groupValues[1].trim()
                return "Updating your display name to **$newName**." to listOf(
                    BotActionDirective(action = "update_name", text = newName)
                )
            }

        // 12. Redeem or clear tag
        Regex("""^redeem\s+(?:tag\s+|code\s+)?(.+)$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)?.let { m ->
                val code = m.groupValues[1].trim()
                return "Processing badge redemption." to listOf(
                    BotActionDirective(action = "redeem_tag", code = code)
                )
            }
        if (raw.equals("clear tag", ignoreCase = true) || raw.equals("remove tag", ignoreCase = true)) {
            return "Clearing your active profile tag." to listOf(
                BotActionDirective(action = "select_tag", tag = "")
            )
        }

        // 13. Read / list chats or contacts
        Regex("""^(?:read|show|list|summarize)\s+(?:my\s+)?(?:chats|conversations|contacts|messages)(?:\s+(?:with|for|in)\s+(.+))?$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw)?.let { m ->
                val target = m.groupValues[1].trim().ifBlank { null }
                return "" to listOf(
                    BotActionDirective(action = "read_chats", peer = target)
                )
            }

        // 14. Sync relay
        if (raw.equals("sync relay", ignoreCase = true) || raw.equals("sync", ignoreCase = true)) {
            return "Syncing Serverless Relay now." to listOf(
                BotActionDirective(action = "sync_relay")
            )
        }

        return null
    }
}
