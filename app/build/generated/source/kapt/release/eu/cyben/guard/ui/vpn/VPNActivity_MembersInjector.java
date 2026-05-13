package eu.cyben.guard.ui.vpn;

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
public final class VPNActivity_MembersInjector implements MembersInjector<VPNActivity> {
  private final Provider<ApiService> apiProvider;

  public VPNActivity_MembersInjector(Provider<ApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  public static MembersInjector<VPNActivity> create(Provider<ApiService> apiProvider) {
    return new VPNActivity_MembersInjector(apiProvider);
  }

  @Override
  public void injectMembers(VPNActivity instance) {
    injectApi(instance, apiProvider.get());
  }

  @InjectedFieldSignature("eu.cyben.guard.ui.vpn.VPNActivity.api")
  public static void injectApi(VPNActivity instance, ApiService api) {
    instance.api = api;
  }
}
