package eu.cyben.guard.ui.dashboard;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import eu.cyben.guard.data.api.ApiService;
import eu.cyben.guard.utils.TokenManager;
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
public final class DashboardActivity_MembersInjector implements MembersInjector<DashboardActivity> {
  private final Provider<ApiService> apiProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public DashboardActivity_MembersInjector(Provider<ApiService> apiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.apiProvider = apiProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  public static MembersInjector<DashboardActivity> create(Provider<ApiService> apiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new DashboardActivity_MembersInjector(apiProvider, tokenManagerProvider);
  }

  @Override
  public void injectMembers(DashboardActivity instance) {
    injectApi(instance, apiProvider.get());
    injectTokenManager(instance, tokenManagerProvider.get());
  }

  @InjectedFieldSignature("eu.cyben.guard.ui.dashboard.DashboardActivity.api")
  public static void injectApi(DashboardActivity instance, ApiService api) {
    instance.api = api;
  }

  @InjectedFieldSignature("eu.cyben.guard.ui.dashboard.DashboardActivity.tokenManager")
  public static void injectTokenManager(DashboardActivity instance, TokenManager tokenManager) {
    instance.tokenManager = tokenManager;
  }
}
