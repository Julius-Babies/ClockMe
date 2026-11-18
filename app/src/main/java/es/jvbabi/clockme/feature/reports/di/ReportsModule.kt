package es.jvbabi.clockme.feature.reports.di

import es.jvbabi.clockme.feature.reports.pdf.PdfReportExporter
import es.jvbabi.clockme.feature.reports.pdf.ReportExporter
import es.jvbabi.clockme.feature.reports.ui.ReportsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reportsModule = module {
    single<ReportExporter> { PdfReportExporter(androidContext()) }
    viewModelOf(::ReportsViewModel)
}
