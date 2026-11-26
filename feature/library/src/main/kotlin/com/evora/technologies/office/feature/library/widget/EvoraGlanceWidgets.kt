package com.evora.technologies.office.feature.library.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.evora.technologies.office.core.designsystem.R
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.WidgetRefreshPort
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

object EvoraWidgetIntents {
    const val ACTION_WIDGET_SEARCH = "com.evora.technologies.office.ACTION_WIDGET_SEARCH"
    const val ACTION_WIDGET_NEW_DOC = "com.evora.technologies.office.ACTION_WIDGET_NEW_DOC"
    const val ACTION_WIDGET_OPEN_DOC = "com.evora.technologies.office.ACTION_WIDGET_OPEN_DOC"
    const val ACTION_SCAN = "com.evora.technologies.office.ACTION_SCAN"

    const val EXTRA_DOC_URI = "extra_doc_uri"
    const val EXTRA_DOC_NAME = "extra_doc_name"
    const val EXTRA_DOC_FORMAT = "extra_doc_format"

    fun buildLaunchIntent(context: Context, action: String): Intent =
        Intent(action).apply {
            setPackage(context.packageName)
            setClassName(context.packageName, "com.evora.technologies.office.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

    fun buildOpenDocumentIntent(context: Context, doc: DocumentItem): Intent =
        buildLaunchIntent(context, ACTION_WIDGET_OPEN_DOC).apply {
            // Distinct data URI prevents PendingIntent.filterEquals collisions across 3 widget rows
            data = Uri.parse("evora://widget/open?uri=${Uri.encode(doc.uriString)}")
            putExtra(EXTRA_DOC_URI, doc.uriString)
            putExtra(EXTRA_DOC_NAME, doc.displayName)
            putExtra(EXTRA_DOC_FORMAT, doc.format.name)
        }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun documentRepository(): DocumentRepository
}

/**
 * W-01 Quick Bar Widget (4x1): EO badge, Search pill, Scan CTA, and New Template CTA.
 */
class QuickBarGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val searchLabel = context.getString(R.string.evora_widget_search_hint)
        val scanLabel = context.getString(R.string.evora_action_scan)
        val newLabel = context.getString(R.string.evora_widget_new_doc)

        provideContent {
            GlanceTheme {
                QuickBarWidgetContent(
                    context = context,
                    searchLabel = searchLabel,
                    scanLabel = scanLabel,
                    newLabel = newLabel,
                )
            }
        }
    }
}

@Composable
private fun QuickBarWidgetContent(
    context: Context,
    searchLabel: String,
    scanLabel: String,
    newLabel: String,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(40.dp)
                .background(GlanceTheme.colors.primary)
                .cornerRadius(10.dp)
                .clickable(actionStartActivity(EvoraWidgetIntents.buildLaunchIntent(context, EvoraWidgetIntents.ACTION_WIDGET_SEARCH))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "EO",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .height(48.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity(EvoraWidgetIntents.buildLaunchIntent(context, EvoraWidgetIntents.ACTION_WIDGET_SEARCH)))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = searchLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontSize = 13.sp,
                ),
                maxLines = 1,
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        Box(
            modifier = GlanceModifier
                .height(48.dp)
                .background(GlanceTheme.colors.primary)
                .cornerRadius(12.dp)
                .clickable(actionStartActivity(EvoraWidgetIntents.buildLaunchIntent(context, EvoraWidgetIntents.ACTION_SCAN)))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = scanLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }

        Spacer(modifier = GlanceModifier.width(6.dp))

        Box(
            modifier = GlanceModifier
                .height(48.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(12.dp)
                .clickable(actionStartActivity(EvoraWidgetIntents.buildLaunchIntent(context, EvoraWidgetIntents.ACTION_WIDGET_NEW_DOC)))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = newLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }
    }
}

/**
 * W-02 Recent Documents Widget (4x2): Header w/ Scan + top 3 recent files w/ format badge & progress.
 */
class RecentDocsGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        val recents = runCatching {
            entryPoint.documentRepository().observeRecentDocuments().first().take(3)
        }.getOrDefault(emptyList())

        val title = context.getString(R.string.evora_widget_recent_name)
        val scanLabel = context.getString(R.string.evora_action_scan)
        val emptyMsg = context.getString(R.string.evora_widget_recent_empty)

        provideContent {
            GlanceTheme {
                RecentDocsWidgetContent(
                    context = context,
                    title = title,
                    scanLabel = scanLabel,
                    emptyMessage = emptyMsg,
                    recentDocs = recents,
                )
            }
        }
    }
}

@Composable
private fun RecentDocsWidgetContent(
    context: Context,
    title: String,
    scanLabel: String,
    emptyMessage: String,
    recentDocs: List<DocumentItem>,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Evora Office",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                ),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            Box(
                modifier = GlanceModifier
                    .height(36.dp)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(10.dp)
                    .clickable(actionStartActivity(EvoraWidgetIntents.buildLaunchIntent(context, EvoraWidgetIntents.ACTION_SCAN)))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = scanLabel,
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    ),
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (recentDocs.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(EvoraWidgetIntents.buildLaunchIntent(context, EvoraWidgetIntents.ACTION_WIDGET_SEARCH))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emptyMessage,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                )
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                recentDocs.forEach { doc ->
                    val badgeBg = formatColorInt(doc.format)
                    val progress = if (doc.lastTotalPages > 0) {
                        "Page ${doc.lastPageIndex + 1}/${doc.lastTotalPages}"
                    } else {
                        doc.extension.uppercase()
                    }
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(actionStartActivity(EvoraWidgetIntents.buildOpenDocumentIntent(context, doc))),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(34.dp)
                                .background(ColorProvider(androidx.compose.ui.graphics.Color(badgeBg)))
                                .cornerRadius(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = doc.extension.uppercase().take(4),
                                style = TextStyle(
                                    color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                ),
                                maxLines = 1,
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(10.dp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = doc.displayName,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                ),
                                maxLines = 1,
                            )
                            Text(
                                text = progress,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 11.sp,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatColorInt(format: DocFormat): Long = when (format) {
    DocFormat.PDF -> 0xFFD33434L
    DocFormat.WORD -> 0xFF1563DBL
    DocFormat.EXCEL -> 0xFF1D820EL
    DocFormat.PPT -> 0xFFB85600L
    DocFormat.HWP -> 0xFF0074BFL
    DocFormat.ODF -> 0xFF8B1ED9L
    DocFormat.EPUB -> 0xFF5A7D0EL
    DocFormat.TEXT, DocFormat.ARCHIVE -> 0xFF1474B0L
}

class QuickBarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickBarGlanceWidget()
}

class RecentDocsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecentDocsGlanceWidget()
}

@Singleton
class GlanceWidgetRefreshAdapter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : WidgetRefreshPort {
    override suspend fun refreshAllWidgets() {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(QuickBarGlanceWidget::class.java).forEach { id ->
                QuickBarGlanceWidget().update(context, id)
            }
            manager.getGlanceIds(RecentDocsGlanceWidget::class.java).forEach { id ->
                RecentDocsGlanceWidget().update(context, id)
            }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryWidgetModule {
    @Binds
    @Singleton
    abstract fun bindWidgetRefreshPort(impl: GlanceWidgetRefreshAdapter): WidgetRefreshPort
}
