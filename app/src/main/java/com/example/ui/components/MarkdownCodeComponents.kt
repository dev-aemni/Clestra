package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageEntity
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraNeuSurface
import com.example.ui.theme.ClestraNeuWell
import com.example.ui.theme.ClestraOnline
import com.example.ui.theme.ClestraTextSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface MarkdownSegment {
    data class TextSegment(val text: String) : MarkdownSegment
    data class CodeBlockSegment(val language: String, val code: String) : MarkdownSegment
}

data class FullScreenDocumentData(
    val title: String,
    val language: String,
    val content: String,
    val sourceUri: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null
)

object MarkdownParser {
    private val fenceRegex = Regex("```([A-Za-z0-9_+#.-]*)\\s*\\n([\\s\\S]*?)```")

    fun parseBlocks(raw: String): List<MarkdownSegment> {
        if (raw.isEmpty()) return emptyList()
        val result = mutableListOf<MarkdownSegment>()
        var lastIndex = 0
        for (match in fenceRegex.findAll(raw)) {
            if (match.range.first > lastIndex) {
                val before = raw.substring(lastIndex, match.range.first).trim('\n')
                if (before.isNotBlank()) {
                    result.add(MarkdownSegment.TextSegment(before))
                }
            }
            val lang = match.groupValues[1].trim().ifBlank { "code" }
            val code = match.groupValues[2].trimEnd('\n')
            result.add(MarkdownSegment.CodeBlockSegment(lang, code))
            lastIndex = match.range.last + 1
        }
        if (lastIndex < raw.length) {
            val tail = raw.substring(lastIndex).trim('\n')
            if (tail.isNotBlank()) {
                result.add(MarkdownSegment.TextSegment(tail))
            }
        }
        if (result.isEmpty() && raw.isNotBlank()) {
            result.add(MarkdownSegment.TextSegment(raw))
        }
        return result
    }

    fun formatInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        val inner = text.substring(i + 2, end)
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(inner)
                        pop()
                        i = end + 2
                        continue
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1 && end > i + 1) {
                        val inner = text.substring(i + 1, end)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                background = Color(0x222C2018),
                                color = Color(0xFF7D3E12)
                            )
                        )
                        append(" $inner ")
                        pop()
                        i = end + 1
                        continue
                    }
                }
                text[i] == '*' && (i + 1 < text.length && text[i + 1] != ' ' && text[i + 1] != '*') -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1 && end > i + 1) {
                        val inner = text.substring(i + 1, end)
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(inner)
                        pop()
                        i = end + 1
                        continue
                    }
                }
            }
            append(text[i])
            i++
        }
    }
}

object SyntaxHighlighter {
    private val keywords = setOf(
        "fun", "val", "var", "class", "object", "interface", "enum", "sealed", "data",
        "override", "private", "public", "internal", "protected", "open", "suspend",
        "return", "if", "else", "when", "for", "while", "do", "try", "catch", "finally",
        "throw", "import", "package", "true", "false", "null", "this", "super", "in", "is", "as",
        "def", "lambda", "with", "yield", "async", "await", "from", "None", "True", "False",
        "function", "const", "let", "export", "default", "new", "typeof", "void", "switch", "case",
        "break", "continue", "struct", "impl", "fn", "pub", "use", "mod", "match", "mut",
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "DELETE", "JOIN", "ON",
        "select", "from", "where", "insert", "into", "values", "update", "delete", "join", "on"
    )

    private val colorDefault = Color(0xFFF5EFE7)
    private val colorKeyword = Color(0xFFF4A261)
    private val colorString = Color(0xFF95D5B2)
    private val colorComment = Color(0xFF9A8C82)
    private val colorNumber = Color(0xFFFF8C70)
    private val colorTypeOrTag = Color(0xFF7BDFF2)
    private val colorAnnotation = Color(0xFFE9C46A)

    fun highlight(code: String, language: String): AnnotatedString = buildAnnotatedString {
        append(code)
        addStyle(SpanStyle(color = colorDefault), 0, code.length)

        val lang = language.lowercase()

        // Numbers
        Regex("\\b\\d+(\\.\\d+)?\\b").findAll(code).forEach {
            addStyle(SpanStyle(color = colorNumber), it.range.first, it.range.last + 1)
        }

        // Types / PascalCase / HTML tags
        Regex("\\b[A-Z][A-Za-z0-9_]*\\b").findAll(code).forEach {
            addStyle(SpanStyle(color = colorTypeOrTag), it.range.first, it.range.last + 1)
        }
        if (lang in setOf("html", "xml", "htm")) {
            Regex("</?[A-Za-z0-9:_-]+").findAll(code).forEach {
                addStyle(SpanStyle(color = colorTypeOrTag, fontWeight = FontWeight.SemiBold), it.range.first, it.range.last + 1)
            }
        }

        // Annotations (@Composable, @Decorator)
        Regex("@[A-Za-z_][A-Za-z0-9_]*").findAll(code).forEach {
            addStyle(SpanStyle(color = colorAnnotation), it.range.first, it.range.last + 1)
        }

        // Keywords
        Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b").findAll(code).forEach {
            if (it.value in keywords) {
                addStyle(
                    SpanStyle(color = colorKeyword, fontWeight = FontWeight.SemiBold),
                    it.range.first,
                    it.range.last + 1
                )
            }
        }

        // Strings ("..." and '...')
        Regex("(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')").findAll(code).forEach {
            addStyle(SpanStyle(color = colorString), it.range.first, it.range.last + 1)
        }

        // Line & block comments override inside spans
        Regex("//[^\\n]*").findAll(code).forEach {
            addStyle(SpanStyle(color = colorComment, fontStyle = FontStyle.Italic), it.range.first, it.range.last + 1)
        }
        if (lang in setOf("py", "python", "sh", "bash", "yaml", "yml", "rb", "toml", "properties")) {
            Regex("#[^\\n]*").findAll(code).forEach {
                addStyle(SpanStyle(color = colorComment, fontStyle = FontStyle.Italic), it.range.first, it.range.last + 1)
            }
        }
        Regex("/\\*[\\s\\S]*?\\*/").findAll(code).forEach {
            addStyle(SpanStyle(color = colorComment, fontStyle = FontStyle.Italic), it.range.first, it.range.last + 1)
        }
    }
}

@Composable
fun MarkdownMessageContent(
    rawText: String,
    textColor: Color = ClestraCharcoal,
    onOpenFullScreenCode: (FullScreenDocumentData) -> Unit
) {
    val blocks = remember(rawText) { MarkdownParser.parseBlocks(rawText) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownSegment.TextSegment -> {
                    val lines = block.text.lines()
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        lines.forEach { line ->
                            val trimmed = line.trimStart()
                            when {
                                trimmed.startsWith("### ") || trimmed.startsWith("## ") || trimmed.startsWith("# ") -> {
                                    val headingText = trimmed.trimStart('#').trim()
                                    Text(
                                        text = MarkdownParser.formatInlineMarkdown(headingText),
                                        color = textColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(
                                            text = "• ",
                                            color = ClestraCopper,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = MarkdownParser.formatInlineMarkdown(trimmed.drop(2)),
                                            color = textColor,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                                else -> {
                                    Text(
                                        text = MarkdownParser.formatInlineMarkdown(line),
                                        color = textColor,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }

                is MarkdownSegment.CodeBlockSegment -> {
                    CollapsedCodePreviewCard(
                        language = block.language,
                        fullCode = block.code,
                        onExpandFullScreen = {
                            onOpenFullScreenCode(
                                FullScreenDocumentData(
                                    title = "${block.language.uppercase()} Snippet",
                                    language = block.language,
                                    content = block.code
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CollapsedCodePreviewCard(
    language: String,
    fullCode: String,
    titleOverride: String? = null,
    onExpandFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    val allLines = remember(fullCode) { fullCode.lines() }
    val previewLimit = 5
    val previewText = remember(fullCode) {
        allLines.take(previewLimit).joinToString("\n")
    }
    val hasMoreLines = allLines.size > previewLimit
    val highlighted = remember(previewText, language) {
        SyntaxHighlighter.highlight(previewText, language)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1F1B18))
    ) {
        // Header bar with language badge + Copy + Full-screen buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2B2521))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = (titleOverride ?: language).uppercase(),
                        color = Color(0xFF14110F),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (hasMoreLines) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${allLines.size} lines",
                        color = Color(0xFFB5A79C),
                        fontSize = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3A322D))
                        .clickable {
                            clipboard.setText(AnnotatedString(fullCode))
                            copied = true
                            Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                delay(1500)
                                copied = false
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = if (copied) ClestraOnline else Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (copied) "Copied" else "Copy",
                            color = if (copied) ClestraOnline else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { onExpandFullScreen() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Full screen code",
                            tint = Color(0xFF14110F),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Full",
                            color = Color(0xFF14110F),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Boxed preview lines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = highlighted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
        }

        if (hasMoreLines) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF26201C))
                    .clickable { onExpandFullScreen() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Previewing ${previewLimit} of ${allLines.size} lines",
                    color = Color(0xFFB5A79C),
                    fontSize = 10.sp
                )
                Text(
                    text = "Open Full Screen →",
                    color = Color(0xFFF4A261),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun FileAttachmentCard(
    message: MessageEntity,
    onOpenFullScreenFile: (FullScreenDocumentData) -> Unit,
    onDownloadFile: (uri: String, fileName: String, mimeType: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fileName = message.attachmentName?.ifBlank { null } ?: message.content.ifBlank { "attachment" }
    val kind = remember(fileName, message.attachmentMimeType) {
        FileAttachmentUtils.classifyFileKind(fileName, message.attachmentMimeType)
    }
    val sizeText = remember(message.attachmentSizeBytes) {
        FileAttachmentUtils.formatFileSize(message.attachmentSizeBytes)
    }

    var loadedTextContent by remember(message.mediaUri) { mutableStateOf<String?>(null) }
    var audioInfo by remember(message.mediaUri) { mutableStateOf<Pair<Long, List<Int>>?>(null) }
    LaunchedEffect(message.mediaUri, kind) {
        if (kind == ClestraFileKind.CODE_OR_TEXT && !message.mediaUri.isNullOrBlank()) {
            loadedTextContent = FileAttachmentUtils.readTextFileContent(context, message.mediaUri)
        } else if (kind == ClestraFileKind.AUDIO && !message.mediaUri.isNullOrBlank()) {
            audioInfo = AudioMetaCodec.inspectAudioFile(context, message.mediaUri, fileName, bars = 24)
        }
    }

    val (icon, accentColor) = fileIconAndColor(kind)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicPressed(cornerRadius = 16.dp, containerColor = ClestraNeuWell)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .neumorphicRaised(
                        cornerRadius = 12.dp,
                        elevation = 4.dp,
                        containerColor = ClestraNeuSurface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "${kind.badgeLabel} file",
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    color = ClestraCharcoal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SoftUiBadgeTag(label = kind.badgeLabel)
                    Text(
                        text = sizeText,
                        color = ClestraTextSoft,
                        fontSize = 11.sp
                    )
                }
            }

            if (!message.mediaUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { onDownloadFile(message.mediaUri, fileName, message.attachmentMimeType) },
                    modifier = Modifier
                        .size(36.dp)
                        .neumorphicRaised(
                            cornerRadius = 11.dp,
                            elevation = 4.dp,
                            containerColor = ClestraNeuSurface
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Save file",
                        tint = Color(0xFF14110F),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Compact inline audio preview player for MP3, WAV, M4A, AAC, OGG, FLAC
        if (kind == ClestraFileKind.AUDIO && !message.mediaUri.isNullOrBlank()) {
            val info = audioInfo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicRaised(
                        cornerRadius = 16.dp,
                        elevation = 3.5.dp,
                        containerColor = ClestraNeuSurface
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                ClestraAudioPlayerBar(
                    uriString = message.mediaUri,
                    waveform = info?.second ?: emptyList(),
                    fallbackDurationMs = info?.first ?: 0L,
                    accentColor = accentColor,
                    surfaceColor = ClestraNeuSurface
                )
            }
        }

        // For Text / Code files: collapsed preview box with Copy & Full-screen viewer!
        val textPreview = loadedTextContent
        if (kind == ClestraFileKind.CODE_OR_TEXT && !textPreview.isNullOrBlank()) {
            val lang = remember(fileName) { FileAttachmentUtils.inferCodeLanguage(fileName) }
            CollapsedCodePreviewCard(
                language = lang,
                titleOverride = fileName,
                fullCode = textPreview,
                onExpandFullScreen = {
                    onOpenFullScreenFile(
                        FullScreenDocumentData(
                            title = fileName,
                            language = lang,
                            content = textPreview,
                            sourceUri = message.mediaUri,
                            fileName = fileName,
                            mimeType = message.attachmentMimeType
                        )
                    )
                }
            )
        }
    }
}

private fun fileIconAndColor(kind: ClestraFileKind): Pair<ImageVector, Color> = when (kind) {
    ClestraFileKind.APK -> Icons.Default.Android to Color(0xFF2E9E5B)
    ClestraFileKind.CODE_OR_TEXT -> Icons.Default.Code to Color(0xFFC4763A)
    ClestraFileKind.PDF -> Icons.Default.PictureAsPdf to Color(0xFFD94343)
    ClestraFileKind.ARCHIVE -> Icons.Default.FolderZip to Color(0xFF8C5E3C)
    ClestraFileKind.AUDIO -> Icons.Default.AudioFile to Color(0xFF6B5CE7)
    ClestraFileKind.VIDEO -> Icons.Default.VideoFile to Color(0xFF2B7FD9)
    ClestraFileKind.IMAGE -> Icons.Default.Image to Color(0xFFC4763A)
    ClestraFileKind.UNKNOWN -> Icons.Default.QuestionMark to Color(0xFF5D534B)
}

@Composable
fun FullScreenCodeViewerModal(
    doc: FullScreenDocumentData,
    onClose: () -> Unit,
    onDownloadFile: ((uri: String, fileName: String, mimeType: String?) -> Unit)? = null
) {
    BackHandler { onClose() }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    val lines = remember(doc.content) { doc.content.lines() }
    val highlighted = remember(doc.content, doc.language) {
        SyntaxHighlighter.highlight(doc.content, doc.language)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF191614))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF241F1C))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF332C28))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close viewer",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${doc.language.uppercase()} • ${lines.size} lines",
                    color = Color(0xFFB5A79C),
                    fontSize = 11.sp
                )
            }

            if (doc.sourceUri != null && doc.fileName != null && onDownloadFile != null) {
                IconButton(
                    onClick = { onDownloadFile(doc.sourceUri, doc.fileName, doc.mimeType) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF332C28))
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Save file",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable {
                        clipboard.setText(AnnotatedString(doc.content))
                        copied = true
                        Toast.makeText(context, "Copied all", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy all",
                        tint = Color(0xFF14110F),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        color = Color(0xFF14110F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            // Line numbers column
            Column(
                modifier = Modifier.padding(end = 12.dp),
                horizontalAlignment = Alignment.End
            ) {
                lines.forEachIndexed { idx, _ ->
                    Text(
                        text = "${idx + 1}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        color = Color(0xFF7D7066)
                    )
                }
            }

            // Full syntax-highlighted content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = highlighted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}
