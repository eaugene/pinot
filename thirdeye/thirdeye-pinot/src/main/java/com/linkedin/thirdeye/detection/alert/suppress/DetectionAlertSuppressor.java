package com.linkedin.thirdeye.detection.alert.suppress;

import com.linkedin.thirdeye.detection.alert.DetectionAlertFilterResult;


/**
 * The base alert suppressor whose purpose is to suppress the  actual  alert
 * and ensure no alerts/notifications are sent to the recipients. Alerts may
 * be suppressed or delayed on various occasions to cut down the alert noise
 * especially triggered during deployments, holidays, etc.
 */
public abstract class DetectionAlertSuppressor {

  public abstract DetectionAlertFilterResult run(DetectionAlertFilterResult result) throws Exception;
}
