package eu.cyben.guard.ui.settings;

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
public final class SettingsActivity_MembersInjector implements MembersInjector<SettingsActivity> {
  private final Provider<ApiService> apiProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public SettingsActivity_MembersInjector(Provider<ApiService> apiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.apiProvider = apiProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  public static MembersInjector<SettingsActivity> create(Provider<ApiService> apiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new SettingsActivity_MembersInjector(apiProvider, tokenManagerProvider);
  }

  @Override
  public void injectMembers(SettingsActivity instance) {
    injectApi(instance, apiProvider.get());
    injectTokenManager(instance, tokenManagerProvider.get());
  }

  @InjectedFieldSignature("eu.cyben.guard.ui.settings.SettingsActivity.api")
  public static void injectApi(SettingsActivity instance, ApiService api) {
    instance.api = api;
  }

  @InjectedFieldSignature("eu.cyben.guard.ui.settings.SettingsActivity.tokenManager")
  public static void injectTokenManager(SettingsActivity instance, TokenManager tokenManager) {
    instance.tokenManager = tokenManager;
  }
}
