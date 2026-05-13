package eu.cyben.guard.ui.prohmed;

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
public final class ProhmedActivity_MembersInjector implements MembersInjector<ProhmedActivity> {
  private final Provider<ApiService> apiProvider;

  public ProhmedActivity_MembersInjector(Provider<ApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  public static MembersInjector<ProhmedActivity> create(Provider<ApiService> apiProvider) {
    return new ProhmedActivity_MembersInjector(apiProvider);
  }

  @Override
  public void injectMembers(ProhmedActivity instance) {
    injectApi(instance, apiProvider.get());
  }

  @InjectedFieldSignature("eu.cyben.guard.ui.prohmed.ProhmedActivity.api")
  public static void injectApi(ProhmedActivity instance, ApiService api) {
    instance.api = api;
  }
}
