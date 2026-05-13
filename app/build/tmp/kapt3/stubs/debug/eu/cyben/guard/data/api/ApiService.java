package eu.cyben.guard.data.api;

import eu.cyben.guard.data.models.*;
import retrofit2.Response;
import retrofit2.http.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u00de\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\u001e\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u001e\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\nH\u00a7@\u00a2\u0006\u0002\u0010\u000bJ\u001e\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010\u000fJ\u001e\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u00032\b\b\u0001\u0010\u0012\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u0010\u0014J\u001e\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00160\u00032\b\b\u0001\u0010\u0017\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u0010\u0014J\u001e\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00190\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u001aH\u00a7@\u00a2\u0006\u0002\u0010\u001bJ\u001a\u0010\u001c\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001e0\u001d0\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u001fJ\u0014\u0010 \u001a\b\u0012\u0004\u0012\u00020!0\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u001fJ\u001a\u0010\"\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020#0\u001d0\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u001fJ\u001a\u0010$\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020%0\u001d0\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u001fJ\u0014\u0010&\u001a\b\u0012\u0004\u0012\u00020\'0\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u001fJ\u001a\u0010(\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\u001d0\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u001fJ\u0014\u0010)\u001a\b\u0012\u0004\u0012\u00020*0\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u001fJ\u0014\u0010+\u001a\b\u0012\u0004\u0012\u00020,0\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u001fJ\u0014\u0010-\u001a\b\u0012\u0004\u0012\u00020.0\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u001fJ\u001e\u0010/\u001a\b\u0012\u0004\u0012\u0002000\u00032\b\b\u0001\u0010\u0005\u001a\u000201H\u00a7@\u00a2\u0006\u0002\u00102J\u001e\u00103\u001a\b\u0012\u0004\u0012\u0002000\u00032\b\b\u0001\u0010\u0005\u001a\u000204H\u00a7@\u00a2\u0006\u0002\u00105J\u001e\u00106\u001a\b\u0012\u0004\u0012\u0002070\u00032\b\b\u0001\u0010\u0005\u001a\u000208H\u00a7@\u00a2\u0006\u0002\u00109J\u001e\u0010:\u001a\b\u0012\u0004\u0012\u00020;0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u001aH\u00a7@\u00a2\u0006\u0002\u0010\u001bJ\u001e\u0010<\u001a\b\u0012\u0004\u0012\u00020=0\u00032\b\b\u0001\u0010\u0005\u001a\u00020>H\u00a7@\u00a2\u0006\u0002\u0010?J\u001e\u0010@\u001a\b\u0012\u0004\u0012\u00020A0\u00032\b\b\u0001\u0010\u0005\u001a\u00020BH\u00a7@\u00a2\u0006\u0002\u0010CJ\u001e\u0010D\u001a\b\u0012\u0004\u0012\u00020E0\u00032\b\b\u0001\u0010\u0005\u001a\u00020FH\u00a7@\u00a2\u0006\u0002\u0010GJ\u0014\u0010H\u001a\b\u0012\u0004\u0012\u00020I0\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u001f\u00a8\u0006J"}, d2 = {"Leu/cyben/guard/data/api/ApiService;", "", "activateProhmed", "Lretrofit2/Response;", "Leu/cyben/guard/data/models/ProhmedActivateResponse;", "body", "Leu/cyben/guard/data/models/ProhmedActivateRequest;", "(Leu/cyben/guard/data/models/ProhmedActivateRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "addMonitoredEmail", "Leu/cyben/guard/data/models/GuardMonitoredEmail;", "Leu/cyben/guard/data/api/AddEmailRequest;", "(Leu/cyben/guard/data/api/AddEmailRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "analyze", "Leu/cyben/guard/data/models/AnalyzeResponse;", "Leu/cyben/guard/data/api/AnalyzeRequest;", "(Leu/cyben/guard/data/api/AnalyzeRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "checkBreach", "Leu/cyben/guard/data/models/BreachCheckResponse;", "emailId", "", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteMonitoredEmail", "", "id", "forgotPassword", "Leu/cyben/guard/data/api/MessageResponse;", "Leu/cyben/guard/data/api/EmailRequest;", "(Leu/cyben/guard/data/api/EmailRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAnalyses", "", "Leu/cyben/guard/data/models/GuardAnalysis;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getBillingPortal", "Leu/cyben/guard/data/models/BillingPortalResponse;", "getBreachAlerts", "Leu/cyben/guard/data/models/GuardBreachAlert;", "getConsults", "Leu/cyben/guard/data/models/ProhmedConsult;", "getMe", "Leu/cyben/guard/data/models/GuardUser;", "getMonitoredEmails", "getProhmedStatus", "Leu/cyben/guard/data/models/ProhmedStatus;", "getVPNCredentials", "Leu/cyben/guard/data/models/VPNCredentials;", "getVPNDnsStats", "Leu/cyben/guard/data/models/VPNDnsStats;", "login", "Leu/cyben/guard/data/models/GuardAuthResponse;", "Leu/cyben/guard/data/api/LoginRequest;", "(Leu/cyben/guard/data/api/LoginRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "register", "Leu/cyben/guard/data/api/RegisterRequest;", "(Leu/cyben/guard/data/api/RegisterRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "requestHuman", "Leu/cyben/guard/data/models/HumanRequestResponse;", "Leu/cyben/guard/data/api/HumanRequest;", "(Leu/cyben/guard/data/api/HumanRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "resendVerification", "Leu/cyben/guard/data/api/OkResponse;", "sendConsult", "Leu/cyben/guard/data/models/ProhmedConsultResponse;", "Leu/cyben/guard/data/models/ProhmedConsultRequest;", "(Leu/cyben/guard/data/models/ProhmedConsultRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendSOS", "Leu/cyben/guard/data/models/CriticalRequestResponse;", "Leu/cyben/guard/data/api/SOSRequest;", "(Leu/cyben/guard/data/api/SOSRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "subscribe", "Leu/cyben/guard/data/models/SubscribeResponse;", "Leu/cyben/guard/data/api/SubscribeRequest;", "(Leu/cyben/guard/data/api/SubscribeRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "syncSubscription", "Leu/cyben/guard/data/models/SyncResponse;", "app_debug"})
public abstract interface ApiService {
    
    @retrofit2.http.POST(value = "/api/guard/auth/register")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object register(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.RegisterRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.GuardAuthResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/auth/login")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object login(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.LoginRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.GuardAuthResponse>> $completion);
    
    @retrofit2.http.GET(value = "/api/guard/auth/me")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMe(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.GuardUser>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/auth/resend-verification")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object resendVerification(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.EmailRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.api.OkResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/auth/forgot-password")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object forgotPassword(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.EmailRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.api.MessageResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/analyze")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object analyze(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.AnalyzeRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.AnalyzeResponse>> $completion);
    
    @retrofit2.http.GET(value = "/api/guard/analyses")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAnalyses(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<java.util.List<eu.cyben.guard.data.models.GuardAnalysis>>> $completion);
    
    @retrofit2.http.GET(value = "/api/guard/monitored-emails")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMonitoredEmails(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<java.util.List<eu.cyben.guard.data.models.GuardMonitoredEmail>>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/monitored-emails")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object addMonitoredEmail(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.AddEmailRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.GuardMonitoredEmail>> $completion);
    
    @retrofit2.http.DELETE(value = "/api/guard/monitored-emails/{id}")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteMonitoredEmail(@retrofit2.http.Path(value = "id")
    int id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<kotlin.Unit>> $completion);
    
    @retrofit2.http.GET(value = "/api/guard/breach-alerts")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getBreachAlerts(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<java.util.List<eu.cyben.guard.data.models.GuardBreachAlert>>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/breach-check/{emailId}")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object checkBreach(@retrofit2.http.Path(value = "emailId")
    int emailId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.BreachCheckResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/subscribe")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object subscribe(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.SubscribeRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.SubscribeResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/sync-subscription")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object syncSubscription(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.SyncResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/billing-portal")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getBillingPortal(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.BillingPortalResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/human-request")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object requestHuman(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.HumanRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.HumanRequestResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/critical-requests")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object sendSOS(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.SOSRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.CriticalRequestResponse>> $completion);
    
    @retrofit2.http.GET(value = "/api/guard/vpn/credentials")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getVPNCredentials(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.VPNCredentials>> $completion);
    
    @retrofit2.http.GET(value = "/api/guard/vpn/dns-stats")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getVPNDnsStats(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.VPNDnsStats>> $completion);
    
    @retrofit2.http.GET(value = "/api/guard/prohmed/status")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getProhmedStatus(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.ProhmedStatus>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/prohmed/activate")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object activateProhmed(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.models.ProhmedActivateRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.ProhmedActivateResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/prohmed/consult")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object sendConsult(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.models.ProhmedConsultRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.ProhmedConsultResponse>> $completion);
    
    @retrofit2.http.GET(value = "/api/guard/prohmed/consults")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getConsults(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<java.util.List<eu.cyben.guard.data.models.ProhmedConsult>>> $completion);
}