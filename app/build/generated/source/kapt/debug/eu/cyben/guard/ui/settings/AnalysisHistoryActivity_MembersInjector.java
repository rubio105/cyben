package eu.cyben.guard.ui.settings;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import eu.cyben.guard.data.api.ApiService;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class AnalysisHistoryActivity_MembersInjector implements MembersInjector<AnalysisHistoryActivity> {
  private final Provider<ApiService> apiProvider;

  public AnalysisHistoryActivity_MembersInjector(Provider<ApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  public static MembersInjector<AnalysisHistoryActivity> create(Provider<ApiService> apiProvider) {
    return new AnalysisHistoryActivity_MembersInjector(apiProvider);
  }

  @Override
  public void injectMembers(AnalysisHistoryActivity instance) {
    injectApi(instance, apiProvider.get());
  }

  @InjectedFieldSignature("eu.cyben.guard.ui.settings.AnalysisHistoryActivity.api")
  public static void injectApi(AnalysisHistoryActivity instance, ApiService api) {
    instance.api = api;
  }
}
