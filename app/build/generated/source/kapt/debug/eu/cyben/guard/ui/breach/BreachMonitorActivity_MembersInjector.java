package eu.cyben.guard.ui.breach;

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
public final class BreachMonitorActivity_MembersInjector implements MembersInjector<BreachMonitorActivity> {
  private final Provider<ApiService> apiProvider;

  public BreachMonitorActivity_MembersInjector(Provider<ApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  public static MembersInjector<BreachMonitorActivity> create(Provider<ApiService> apiProvider) {
    return new BreachMonitorActivity_MembersInjector(apiProvider);
  }

  @Override
  public void injectMembers(BreachMonitorActivity instance) {
    injectApi(instance, apiProvider.get());
  }

  @InjectedFieldSignature("eu.cyben.guard.ui.breach.BreachMonitorActivity.api")
  public static void injectApi(BreachMonitorActivity instance, ApiService api) {
    instance.api = api;
  }
}
