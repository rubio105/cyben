package eu.cyben.guard.ui.auth;

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
public final class EmailVerificationActivity_MembersInjector implements MembersInjector<EmailVerificationActivity> {
  private final Provider<ApiService> apiProvider;

  public EmailVerificationActivity_MembersInjector(Provider<ApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  public static MembersInjector<EmailVerificationActivity> create(
      Provider<ApiService> apiProvider) {
    return new EmailVerificationActivity_MembersInjector(apiProvider);
  }

  @Override
  public void injectMembers(EmailVerificationActivity instance) {
    injectApi(instance, apiProvider.get());
  }

  @InjectedFieldSignature("eu.cyben.guard.ui.auth.EmailVerificationActivity.api")
  public static void injectApi(EmailVerificationActivity instance, ApiService api) {
    instance.api = api;
  }
}
