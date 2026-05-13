package eu.cyben.guard.data.api;

import eu.cyben.guard.data.models.*;
import retrofit2.Response;
import retrofit2.http.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u008a\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u001e\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u001e\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\nH\u00a7@\u00a2\u0006\u0002\u0010\u000bJ\u001e\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010\u000fJ\u001e\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\r0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u0012J\u001e\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00140\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0015H\u00a7@\u00a2\u0006\u0002\u0010\u0016J\u001e\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00180\u00032\b\b\u0001\u0010\u0019\u001a\u00020\u001aH\u00a7@\u00a2\u0006\u0002\u0010\u001bJ\u001e\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001d0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u001eH\u00a7@\u00a2\u0006\u0002\u0010\u001fJ\u001e\u0010 \u001a\b\u0012\u0004\u0012\u00020!0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\"H\u00a7@\u00a2\u0006\u0002\u0010#J\u0014\u0010$\u001a\b\u0012\u0004\u0012\u00020%0\u0003H\u00a7@\u00a2\u0006\u0002\u0010&J\u001e\u0010\'\u001a\b\u0012\u0004\u0012\u00020%0\u00032\b\b\u0001\u0010(\u001a\u00020\u001aH\u00a7@\u00a2\u0006\u0002\u0010\u001bJ\u001e\u0010)\u001a\b\u0012\u0004\u0012\u00020*0\u00032\b\b\u0001\u0010\u0005\u001a\u00020+H\u00a7@\u00a2\u0006\u0002\u0010,J\u001a\u0010-\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020/0.0\u0003H\u00a7@\u00a2\u0006\u0002\u0010&J\u0014\u00100\u001a\b\u0012\u0004\u0012\u0002010\u0003H\u00a7@\u00a2\u0006\u0002\u0010&J\u001a\u00102\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u0002030.0\u0003H\u00a7@\u00a2\u0006\u0002\u0010&J\u0014\u00104\u001a\b\u0012\u0004\u0012\u0002050\u0003H\u00a7@\u00a2\u0006\u0002\u0010&J\u001a\u00106\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0.0\u0003H\u00a7@\u00a2\u0006\u0002\u0010&J\u001a\u00107\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u0002080.0\u0003H\u00a7@\u00a2\u0006\u0002\u0010&J\u0014\u00109\u001a\b\u0012\u0004\u0012\u00020:0\u0003H\u00a7@\u00a2\u0006\u0002\u0010&J\u0014\u0010;\u001a\b\u0012\u0004\u0012\u00020<0\u0003H\u00a7@\u00a2\u0006\u0002\u0010&J\u0014\u0010=\u001a\b\u0012\u0004\u0012\u00020>0\u0003H\u00a7@\u00a2\u0006\u0002\u0010&J\u001e\u0010?\u001a\b\u0012\u0004\u0012\u00020@0\u00032\b\b\u0001\u0010\u0005\u001a\u00020AH\u00a7@\u00a2\u0006\u0002\u0010BJ\u001e\u0010C\u001a\b\u0012\u0004\u0012\u00020D0\u00032\b\b\u0001\u0010\u0005\u001a\u00020EH\u00a7@\u00a2\u0006\u0002\u0010FJ\u001e\u0010G\u001a\b\u0012\u0004\u0012\u00020D0\u00032\b\b\u0001\u0010\u0005\u001a\u00020HH\u00a7@\u00a2\u0006\u0002\u0010IJ\u001e\u0010J\u001a\b\u0012\u0004\u0012\u00020K0\u00032\b\b\u0001\u0010\u0005\u001a\u00020LH\u00a7@\u00a2\u0006\u0002\u0010MJ\u001e\u0010N\u001a\b\u0012\u0004\u0012\u00020\u00140\u00032\b\b\u0001\u0010\u0005\u001a\u00020+H\u00a7@\u00a2\u0006\u0002\u0010,J\u001e\u0010O\u001a\b\u0012\u0004\u0012\u00020P0\u00032\b\b\u0001\u0010\u0005\u001a\u00020QH\u00a7@\u00a2\u0006\u0002\u0010RJ\u001e\u0010S\u001a\b\u0012\u0004\u0012\u00020T0\u00032\b\b\u0001\u0010\u0005\u001a\u00020UH\u00a7@\u00a2\u0006\u0002\u0010VJ\u0014\u0010W\u001a\b\u0012\u0004\u0012\u00020X0\u0003H\u00a7@\u00a2\u0006\u0002\u0010&J\u001e\u0010Y\u001a\b\u0012\u0004\u0012\u00020D0\u00032\b\b\u0001\u0010\u0005\u001a\u00020ZH\u00a7@\u00a2\u0006\u0002\u0010[\u00a8\u0006\\"}, d2 = {"Leu/cyben/guard/data/api/ApiService;", "", "activateProhmed", "Lretrofit2/Response;", "Leu/cyben/guard/data/models/ProhmedActivateResponse;", "body", "Leu/cyben/guard/data/models/ProhmedActivateRequest;", "(Leu/cyben/guard/data/models/ProhmedActivateRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "addMonitoredEmail", "Leu/cyben/guard/data/models/GuardMonitoredEmail;", "Leu/cyben/guard/data/api/AddEmailRequest;", "(Leu/cyben/guard/data/api/AddEmailRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "analyze", "Leu/cyben/guard/data/models/AnalyzeResponse;", "Leu/cyben/guard/data/api/AnalyzeRequest;", "(Leu/cyben/guard/data/api/AnalyzeRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "analyzeImage", "Leu/cyben/guard/data/models/ImageAnalyzeRequest;", "(Leu/cyben/guard/data/models/ImageAnalyzeRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "changePassword", "Leu/cyben/guard/data/api/OkResponse;", "Leu/cyben/guard/data/models/ChangePasswordRequest;", "(Leu/cyben/guard/data/models/ChangePasswordRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "checkBreach", "Leu/cyben/guard/data/models/BreachCheckResponse;", "emailId", "", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "checkPhone", "Leu/cyben/guard/data/models/PhoneCheckResponse;", "Leu/cyben/guard/data/api/PhoneCheckRequest;", "(Leu/cyben/guard/data/api/PhoneCheckRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "consultProhmed", "Leu/cyben/guard/data/models/ProhmedConsultResponse;", "Leu/cyben/guard/data/models/ProhmedConsultRequest;", "(Leu/cyben/guard/data/models/ProhmedConsultRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteAccount", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteMonitoredEmail", "id", "forgotPassword", "Leu/cyben/guard/data/api/MessageResponse;", "Leu/cyben/guard/data/api/EmailRequest;", "(Leu/cyben/guard/data/api/EmailRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAnalyses", "", "Leu/cyben/guard/data/models/GuardAnalysis;", "getBillingPortal", "Leu/cyben/guard/data/models/BillingPortalResponse;", "getBreachAlerts", "Leu/cyben/guard/data/models/GuardBreachAlert;", "getMe", "Leu/cyben/guard/data/models/GuardUser;", "getMonitoredEmails", "getProhmedConsults", "Leu/cyben/guard/data/models/ProhmedConsult;", "getProhmedStatus", "Leu/cyben/guard/data/models/ProhmedStatus;", "getVPNCredentials", "Leu/cyben/guard/data/models/VPNCredentials;", "getVPNDnsStats", "Leu/cyben/guard/data/models/VPNDnsStats;", "hibpCheck", "Leu/cyben/guard/data/models/HibpCheckResponse;", "Leu/cyben/guard/data/api/HibpCheckRequest;", "(Leu/cyben/guard/data/api/HibpCheckRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "login", "Leu/cyben/guard/data/models/GuardAuthResponse;", "Leu/cyben/guard/data/api/LoginRequest;", "(Leu/cyben/guard/data/api/LoginRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "register", "Leu/cyben/guard/data/api/RegisterRequest;", "(Leu/cyben/guard/data/api/RegisterRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "requestHuman", "Leu/cyben/guard/data/models/HumanRequestResponse;", "Leu/cyben/guard/data/api/HumanRequest;", "(Leu/cyben/guard/data/api/HumanRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "resendVerification", "sendSOS", "Leu/cyben/guard/data/models/CriticalRequestResponse;", "Leu/cyben/guard/data/api/SOSRequest;", "(Leu/cyben/guard/data/api/SOSRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "subscribe", "Leu/cyben/guard/data/models/SubscribeResponse;", "Leu/cyben/guard/data/api/SubscribeRequest;", "(Leu/cyben/guard/data/api/SubscribeRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "syncSubscription", "Leu/cyben/guard/data/models/SyncResponse;", "verifyEmail", "Leu/cyben/guard/data/api/VerifyEmailRequest;", "(Leu/cyben/guard/data/api/VerifyEmailRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
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
    
    @retrofit2.http.DELETE(value = "/api/guard/auth/delete-account")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteAccount(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<kotlin.Unit>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/auth/resend-verification")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object resendVerification(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.EmailRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.api.OkResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/auth/verify-email-code")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object verifyEmail(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.VerifyEmailRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.GuardAuthResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/auth/forgot-password")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object forgotPassword(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.EmailRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.api.MessageResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/auth/change-password")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object changePassword(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.models.ChangePasswordRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.api.OkResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/analyze")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object analyze(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.AnalyzeRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.AnalyzeResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/analyze/image")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object analyzeImage(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.models.ImageAnalyzeRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.AnalyzeResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/check-phone")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object checkPhone(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.PhoneCheckRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.PhoneCheckResponse>> $completion);
    
    @retrofit2.http.POST(value = "/api/guard/hibp-check")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object hibpCheck(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.api.HibpCheckRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.HibpCheckResponse>> $completion);
    
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
    public abstract java.lang.Object consultProhmed(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    eu.cyben.guard.data.models.ProhmedConsultRequest body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<eu.cyben.guard.data.models.ProhmedConsultResponse>> $completion);
    
    @retrofit2.http.GET(value = "/api/guard/prohmed/consults")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getProhmedConsults(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<java.util.List<eu.cyben.guard.data.models.ProhmedConsult>>> $completion);
}