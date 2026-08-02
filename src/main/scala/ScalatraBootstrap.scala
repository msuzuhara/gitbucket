import java.util.EnumSet
import jakarta.servlet._

import gitbucket.core.controller.{ReleaseController, _}
import gitbucket.core.service.SystemSettingsService
import gitbucket.core.servlet._
import gitbucket.core.util.Directory
import org.scalatra._

class ScalatraBootstrap extends LifeCycle with SystemSettingsService {
  override def init(context: ServletContext): Unit = {

    val settings = loadSystemSettings()
    if (settings.baseUrl.exists(_.startsWith("https://"))) {
      context.getSessionCookieConfig.setSecure(true)
    }

    // Register TransactionFilter at first
    context.addFilter("transactionFilter", new TransactionFilter)
    context
      .getFilterRegistration("transactionFilter")
      .addMappingForUrlPatterns(EnumSet.allOf(classOf[DispatcherType]), true, "/*")
    context.addFilter("gitAuthenticationFilter", new GitAuthenticationFilter)
    context
      .getFilterRegistration("gitAuthenticationFilter")
      .addMappingForUrlPatterns(EnumSet.allOf(classOf[DispatcherType]), true, "/git/*")
    context.addFilter("apiAuthenticationFilter", new ApiAuthenticationFilter)
    context
      .getFilterRegistration("apiAuthenticationFilter")
      .addMappingForUrlPatterns(EnumSet.allOf(classOf[DispatcherType]), true, "/api/*")

    // Create composite filter for PreProcessController
    val preProcessFilter = new CompositeScalatraFilter()
    preProcessFilter.mount(new PreProcessController, "/*")
    context.addFilter("preProcessFilter", preProcessFilter)
    context
      .getFilterRegistration("preProcessFilter")
      .addMappingForUrlPatterns(EnumSet.allOf(classOf[DispatcherType]), true, "/*")

    context.addFilter("pluginControllerFilter", new PluginControllerFilter)
    context
      .getFilterRegistration("pluginControllerFilter")
      .addMappingForUrlPatterns(EnumSet.allOf(classOf[DispatcherType]), true, "/*")

    // Create composite filter for FileUploadController
    val uploadFilter = new CompositeScalatraFilter()
    uploadFilter.mount(new FileUploadController, "/upload")
    context.addFilter("uploadFilter", uploadFilter)
    context
      .getFilterRegistration("uploadFilter")
      .addMappingForUrlPatterns(EnumSet.allOf(classOf[DispatcherType]), true, "/upload/*")

    // Create main composite filter
    val mainFilter = new CompositeScalatraFilter()
    mainFilter.mount(new IndexController, "/")
    mainFilter.mount(new ApiController, "/api/v3")
    mainFilter.mount(new SystemSettingsController, "/admin")
    mainFilter.mount(new DashboardController, "/*")
    mainFilter.mount(new AccountController, "/*")
    mainFilter.mount(new RepositoryViewerController, "/*")
    mainFilter.mount(new WikiController, "/*")
    mainFilter.mount(new LabelsController, "/*")
    mainFilter.mount(new PrioritiesController, "/*")
    mainFilter.mount(new MilestonesController, "/*")
    mainFilter.mount(new IssuesController, "/*")
    mainFilter.mount(new PullRequestsController, "/*")
    mainFilter.mount(new ReleaseController, "/*")
    mainFilter.mount(new RepositorySettingsController, "/*")

    context.addFilter("compositeScalatraFilter", mainFilter)
    context
      .getFilterRegistration("compositeScalatraFilter")
      .addMappingForUrlPatterns(EnumSet.allOf(classOf[DispatcherType]), true, "/*")

    // Create GITBUCKET_HOME directory if it does not exist
    val dir = new java.io.File(Directory.GitBucketHome)
    if (!dir.exists) {
      dir.mkdirs()
    }
  }

  override def destroy(context: ServletContext): Unit = {
    Database.closeDataSource()
  }
}
