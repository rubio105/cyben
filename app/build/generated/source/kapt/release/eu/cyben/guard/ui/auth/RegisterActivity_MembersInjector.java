package eu.cyben.guard.ui.auth;

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
public final class RegisterActivity_MembersInjector implements MembersInjector<RegisterActivity> {
  private final Provider<ApiService> apiProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public RegisterActivity_MembersInjector(Provider<ApiService> apiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.apiProvider = apiProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  public static MembersInjector<RegisterActivity> create(Provider<ApiService> apiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new RegisterActivity_MembersInjector(apiProvider, tokenManagerProvider);
  }

  @Override
  public void injectMembers(RegisterActivity instance) {
    injectApi(instance, apiProvider.get());
    injectTokenManager(instance, tokenManagerProvider.get());
  }

  @InjectedFieldSignature("eu.cyben.guard.ui.auth.RegisterActivity.api")
  public static void injectApi(RegisterActivity instance, ApiService api) {
    instance.api = api;
  }

  @InjectedFieldSignature("eu.cyben.guard.ui.auth.RegisterActivity.tokenManager")
  public static void injectTokenManager(RegisterActivity instance, TokenManager tokenManager) {
    instance.tokenManager = tokenManager;
  }
}
