/*
 * Copyright 2016 Karlsruhe Institute of Technology (KIT)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package edu.kit.scc.rest;

import edu.kit.scc.CdmiRestController;
import edu.kit.scc.CdmiServerApplication;

import org.json.JSONObject;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
// import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.HandlerMapping;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CdmiServerApplication.class)
// @ContextConfiguration
public class DataObjectTest {

  @Autowired
  private CdmiRestController controller;

  @Test
  public void A_create() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setServerName("localhost:8080");
    request.setRequestURI("/objectTest");
    request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/objectTest/");
    request.addHeader("Accept", "application/cdmi-object");
    request.addHeader("Content-Type", "application/cdmi-object");
    request.setContent(
        "{ \"value\":\"This file is generated by a test\", \"metadata\" : { created: by test, color:yellow } }"
            .getBytes());
    request.setMethod("PUT");


    controller.putCdmiObject(request.getContentType(),
        "{ \"value\":\"This file is generated by a test\", \"metadata\" : { created: by test, color:yellow } }",
        request, response);
  }

  @Test
  public void A_create_copy() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setServerName("localhost:8080");
    request.setRequestURI("/objectTestCopy");
    request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/objectTestCopy/");
    request.addHeader("Accept", "application/cdmi-object");
    request.addHeader("Content-Type", "application/cdmi-object");
    request.setContent("{copy:\"objectTest\"}".getBytes());
    request.setMethod("PUT");


    controller.putCdmiObject(request.getContentType(), "{copy:\"objectTest\"}", request, response);
  }

  @Test
  public void B_get() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setServerName("localhost:8080");
    request.setRequestURI("/objectTest/");
    request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/objectTest/");
    request.addHeader("Range", "0-10");
    request.setMethod("GET");


    ResponseEntity<?> res = controller.getCdmiObjectByPath(request, response);
    JSONObject json = (JSONObject) res.getBody();
    String content = json.toString();
    String objectId = content.split("objectID\":\"")[1].split("\"")[0];

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    request.setServerName("localhost:8080");
    request.setRequestURI("/cdmi_objectid/" + objectId);
    request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE,
        "/cdmi_objectid/" + objectId);
    request.setMethod("GET");
    request.addHeader("Range", "0-10");
    controller.getCdmiObjectByID(objectId, request, response);
  }

  @Test
  public void C_get_fields() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setServerName("localhost:8080");
    request.setRequestURI("/objectTest/?metadata:color");
    request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/objectTest/");
    request.setParameter("metadata:color", "");
    request.setMethod("GET");


    controller.getCdmiObjectByPath(request, response);

  }

  @Test
  public void C_get_valueRange() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setServerName("localhost:8080");
    request.setRequestURI("/objectTest/?value:2-25");
    request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/objectTest/");
    request.setParameter("value:2-25", "");
    request.setMethod("GET");


    controller.getCdmiObjectByPath(request, response);

  }

  @Test
  public void D_delete() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setServerName("localhost:8080");
    request.setRequestURI("/objectTest/");
    request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/objectTest/");
    request.addHeader("Content-Type", "application/cdmi-object");
    request.setMethod("DELETE");


    controller.deleteCdmiObject(request, response);

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    request.setServerName("localhost:8080");
    request.setRequestURI("/objectTestCopy/");
    request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, "/objectTestCopy/");
    request.addHeader("Content-Type", "application/cdmi-object");
    request.setMethod("DELETE");


    controller.deleteCdmiObject(request, response);

  }


}
