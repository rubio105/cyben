package eu.cyben.guard.ui.subscription;

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
public final class SubscriptionActivity_MembersInjector implements MembersInjector<SubscriptionActivity> {
  private final Provider<ApiService> apiProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public SubscriptionActivity_MembersInjector(Provider<ApiService> apiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.apiProvider = apiProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  public static MembersInjector<SubscriptionActivity> create(Provider<ApiService> apiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new SubscriptionActivity_MembersInjector(apiProvider, tokenManagerProvider);
  }

  @Override
  public void injectMembers(SubscriptionActivity instance) {
    injectApi(instance, apiProvider.get());
    injectTokenManager(instance, tokenManagerProvider.get());
  }

  @InjectedFieldSignature("eu.cyben.guard.ui.subscription.SubscriptionActivity.api")
  public static void injectApi(SubscriptionActivity instance, ApiService api) {
    instance.api = api;
  }

  @InjectedFieldSignature("eu.cyben.guard.ui.subscription.SubscriptionActivity.tokenManager")
  public static void injectTokenManager(SubscriptionActivity instance, TokenManager tokenManager) {
    instance.tokenManager = tokenManager;
  }
}
