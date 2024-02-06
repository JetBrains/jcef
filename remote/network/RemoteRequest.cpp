#include "RemoteRequest.h"
#include "../CefUtils.h"
#include "../handlers/RemoteClientHandler.h"
#include "../log/Log.h"

namespace {
  std::string policy2str(cef_referrer_policy_t policy);
  cef_referrer_policy_t str2policy(std::string policy);
  std::string type2str(cef_resource_type_t type);
  std::string ttype2str(cef_transition_type_t type);
}

void RemoteRequest::updateImpl(const std::map<std::string, std::string>& requestInfo) {
  SET_STR(requestInfo, URL);
  SET_STR(requestInfo, Method);
  SET_INT(requestInfo, Flags);
  SET_STR(requestInfo, FirstPartyForCookies);

  if (requestInfo.count("ReferrerURL") > 0) {
    std::string policyName = requestInfo.count("ReferrerPolicy") ? requestInfo.at("ReferrerPolicy") : "";
    myDelegate->SetReferrer(requestInfo.at("ReferrerURL"),str2policy(policyName));
  }
}

std::map<std::string, std::string> RemoteRequest::toMapImpl() {
    std::map<std::string, std::string> result;
    GET_INT(result, Identifier);
    result["IsReadOnly"] = std::to_string(myDelegate->IsReadOnly());
    GET_STR(result, URL);
    GET_STR(result, Method);
    GET_STR(result, ReferrerURL);
    result["ReferrerPolicy"] = policy2str(myDelegate->GetReferrerPolicy());
    GET_INT(result, Flags);
    GET_STR(result, FirstPartyForCookies);
    result["ResourceType"] = type2str(myDelegate->GetResourceType());
    result["TransitionType"] = ttype2str(myDelegate->GetTransitionType());
    return result;
}

namespace {
  std::pair<cef_referrer_policy_t, std::string> referrerPolicies[] = {
      {REFERRER_POLICY_CLEAR_REFERRER_ON_TRANSITION_FROM_SECURE_TO_INSECURE, "REFERRER_POLICY_CLEAR_REFERRER_ON_TRANSITION_FROM_SECURE_TO_INSECURE"},
      {REFERRER_POLICY_REDUCE_REFERRER_GRANULARITY_ON_TRANSITION_CROSS_ORIGIN, "REFERRER_POLICY_REDUCE_REFERRER_GRANULARITY_ON_TRANSITION_CROSS_ORIGIN"},
      {REFERRER_POLICY_ORIGIN_ONLY_ON_TRANSITION_CROSS_ORIGIN, "REFERRER_POLICY_ORIGIN_ONLY_ON_TRANSITION_CROSS_ORIGIN"},
      {REFERRER_POLICY_NEVER_CLEAR_REFERRER, "REFERRER_POLICY_NEVER_CLEAR_REFERRER"},
      {REFERRER_POLICY_ORIGIN, "REFERRER_POLICY_ORIGIN"},
      {REFERRER_POLICY_CLEAR_REFERRER_ON_TRANSITION_CROSS_ORIGIN, "REFERRER_POLICY_CLEAR_REFERRER_ON_TRANSITION_CROSS_ORIGIN"},
      {REFERRER_POLICY_ORIGIN_CLEAR_ON_TRANSITION_FROM_SECURE_TO_INSECURE, "REFERRER_POLICY_ORIGIN_CLEAR_ON_TRANSITION_FROM_SECURE_TO_INSECURE"},
      {REFERRER_POLICY_NO_REFERRER, "REFERRER_POLICY_NO_REFERRER"}
  };

  std::string policy2str(cef_referrer_policy_t policy) {
    for (auto p: referrerPolicies) {
      if (p.first == policy)
        return p.second;
    }
    return string_format("unknown_policy_%d", policy);
  }

  cef_referrer_policy_t str2policy(std::string policy) {
    for (auto p: referrerPolicies) {
      if (p.second.compare(policy) == 0)
        return p.first;
    }
    return REFERRER_POLICY_CLEAR_REFERRER_ON_TRANSITION_FROM_SECURE_TO_INSECURE; // default
  }

  std::pair<int, std::string> resourceTypes[] = {
      {RT_MAIN_FRAME,"RT_MAIN_FRAME"},
      {RT_SUB_FRAME, "RT_SUB_FRAME"},
      {RT_STYLESHEET, "RT_STYLESHEET"},
      {RT_SCRIPT, "RT_SCRIPT"},
      {RT_IMAGE, "RT_IMAGE"},
      {RT_FONT_RESOURCE, "RT_FONT_RESOURCE"},
      {RT_SUB_RESOURCE, "RT_SUB_RESOURCE"},
      {RT_OBJECT, "RT_OBJECT"},
      {RT_MEDIA, "RT_MEDIA"},
      {RT_WORKER, "RT_WORKER"},
      {RT_SHARED_WORKER, "RT_SHARED_WORKER"},
      {RT_PREFETCH, "RT_PREFETCH"},
      {RT_FAVICON, "RT_FAVICON"},
      {RT_XHR, "RT_XHR"},
      {RT_PING, "RT_PING"},
      {RT_SERVICE_WORKER, "RT_SERVICE_WORKER"},
      {RT_CSP_REPORT, "RT_CSP_REPORT"},
      {RT_PLUGIN_RESOURCE, "RT_PLUGIN_RESOURCE"},
      {RT_NAVIGATION_PRELOAD_MAIN_FRAME, "RT_NAVIGATION_PRELOAD_MAIN_FRAME"},
      {RT_NAVIGATION_PRELOAD_SUB_FRAME, "RT_NAVIGATION_PRELOAD_SUB_FRAME"}
  };

  std::string type2str(cef_resource_type_t type) {
    for (auto p: resourceTypes) {
      if (p.first == type)
        return p.second;
    }
    return string_format("unknown_type_%d", type);
  }

  std::pair<cef_transition_type_t, std::string> transitionTypes[] = {
      {TT_LINK, "TT_LINK"},
      {TT_EXPLICIT, "TT_EXPLICIT"},
      {TT_AUTO_SUBFRAME, "TT_AUTO_SUBFRAME"},
      {TT_MANUAL_SUBFRAME, "TT_MANUAL_SUBFRAME"},
      {TT_FORM_SUBMIT, "TT_FORM_SUBMIT"},
      {TT_RELOAD, "TT_RELOAD"}
  };

  std::string ttype2str(cef_transition_type_t type) {
    for (auto p: transitionTypes) {
      if (p.first == type)
        return p.second;
    }
    return string_format("unknown_transition_type_%d", type);
  }
}

void fillMap(CefRequest::HeaderMap & out, const std::map<std::string, std::string> & in) {
    for (auto it = in.begin(); it != in.end(); ++it)
      out.insert({CefString(it->first), CefString(it->second)});
}

void fillMap(std::map<std::string, std::string> & out, const CefRequest::HeaderMap & in) {
    for (auto it = in.begin(); it != in.end(); ++it)
      out.insert({it->first.ToString(), it->second.ToString()});
}
