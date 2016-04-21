/*
 * Copyright 2016 Karlsruhe Institute of Technology (KIT)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package edu.kit.scc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snia.cdmiserver.dao.filesystem.CapabilityDaoImpl;
import org.snia.cdmiserver.dao.filesystem.ContainerDaoImpl;
import org.snia.cdmiserver.dao.filesystem.DataObjectDaoImpl;
import org.snia.cdmiserver.dao.filesystem.DomainDaoImpl;
import org.snia.cdmiserver.exception.BadRequestException;
import org.snia.cdmiserver.exception.NotFoundException;
import org.snia.cdmiserver.model.Capability;
import org.snia.cdmiserver.model.CdmiObject;
import org.snia.cdmiserver.model.Container;
import org.snia.cdmiserver.model.DataObject;
import org.snia.cdmiserver.model.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@ComponentScan(basePackages = {"edu.kit.scc", "org.snia.cdmiserver"})
public class CdmiRestController {

  private static final Logger log = LoggerFactory.getLogger(CdmiRestController.class);

  @Autowired
  private CapabilityDaoImpl capabilityDaoImpl;

  @Autowired
  private ContainerDaoImpl containerDaoImpl;

  @Autowired
  private DataObjectDaoImpl dataObjectDaoImpl;

  @Autowired
  private DomainDaoImpl domainDaoImpl;

  /**
   * Domains endpoint.
   * 
   * @param request the {@link HttpServletRequest}
   * @param response the {@link HttpServletResponse}
   * @return a JSON serialized {@link Domain} object
   */
  @RequestMapping(path = "/cdmi_domains/**", method = RequestMethod.GET,
      consumes = "application/cdmi-domain+json", produces = "application/cdmi-domain+json")
  public ResponseEntity<?> getDomainByPath(HttpServletRequest request,
      HttpServletResponse response) {
    String path =
        (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

    log.debug("Domain path {}", path);

    String[] requestedFields = parseFields(request);
    CdmiObject domain = domainDaoImpl.findByPath(path);
    response.addHeader("X-CDMI-Specification-Version", "1.1.1");

    if (domain != null) {
      if (requestedFields == null) {
        return new ResponseEntity<JSONObject>(domain.toJson(), HttpStatus.OK);
      } else {
        String jsonString = getRequestedJson(domain.toJson(), requestedFields).toString();
        return new ResponseEntity<String>(jsonString, HttpStatus.OK);
      }
    }

    return new ResponseEntity<String>("Domain not found", HttpStatus.NOT_FOUND);
  }

  /**
   * Capabilities endpoint.
   * 
   * @param request the {@link HttpServletRequest}
   * @param response the {@link HttpServletResponse}
   * @return a JSON serialized {@link Capability} object
   */
  @RequestMapping(path = "/cdmi_capabilities/**", method = RequestMethod.GET,
      consumes = "application/cdmi-capability+json", produces = "application/cdmi-capability+json")
  public ResponseEntity<?> capabilities(HttpServletRequest request, HttpServletResponse response) {
    String path =
        (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    log.debug("Capabilities path {}", path);
    response.addHeader("X-CDMI-Specification-Version", "1.1.1");

    Capability capability = capabilityDaoImpl.findByPath(path);
    String[] requestedFields = parseFields(request);
    if (capability != null) {
      if (requestedFields == null) {
        return new ResponseEntity<JSONObject>(capability.toJson(), HttpStatus.OK);
      } else {
        String jsonString = getRequestedJson(capability.toJson(), requestedFields).toString();
        return new ResponseEntity<String>(jsonString, HttpStatus.OK);
      }
    }
    return new ResponseEntity<String>("Capabilities not found", HttpStatus.NOT_FOUND);
  }

  /**
   * ObjectId endpoint.
   * 
   * @param request the {@link HttpServletRequest}
   * @param response the {@link HttpServletResponse}
   * @return a JSON serialized {@link CdmiObject}
   */
  @RequestMapping(path = "/cdmi_objectid/{objectId}", method = RequestMethod.GET,
      consumes = "application/cdmi-object+json", produces = {"application/cdmi-object+json",
          "application/cdmi-container+json", "application/cdmi-domain+json"})
  public ResponseEntity<?> getCdmiObjectByID(@PathVariable String objectId,
      HttpServletRequest request, HttpServletResponse response) {
    log.debug("Get objectID {}", objectId);
    response.addHeader("X-CDMI-Specification-Version", "1.1.1");

    String[] requestedFields = parseFields(request);
    // look for container
    try {
      CdmiObject container = containerDaoImpl.findByObjectId(objectId);
      if (container != null) {
        response.setContentType("application/cdmi-container+json");
        if (requestedFields == null) {
          return new ResponseEntity<JSONObject>(container.toJson(), HttpStatus.OK);
        } else {
          String jsonString = getRequestedJson(container.toJson(), requestedFields).toString();
          return new ResponseEntity<String>(jsonString, HttpStatus.OK);
        }
      }
    } catch (NotFoundException | ClassCastException e1) {
      // look for dataobject
      try {
        DataObject dataObject = dataObjectDaoImpl.findByObjectId(objectId);
        if (dataObject != null) {
          String range = request.getHeader("Range");
          if (range != null) {
            byte[] content = dataObject.getValue().getBytes();
            String[] ranges = range.split("-");
            try {
              content = Arrays.copyOfRange(content, Integer.valueOf(ranges[0].trim()),
                  Integer.valueOf(ranges[1].trim()));
              dataObject.setValue(new String(content));
            } catch (NumberFormatException numberFormatException) {
              return new ResponseEntity<String>("Bad range", HttpStatus.BAD_REQUEST);
            }
          }
          if (requestedFields == null) {
            return new ResponseEntity<JSONObject>(dataObject.toJson(), HttpStatus.OK);
          } else {
            String jsonString = getRequestedJson(dataObject.toJson(), requestedFields).toString();
            return new ResponseEntity<String>(jsonString, HttpStatus.OK);
          }
        }
      } catch (NotFoundException | ClassCastException e2) {
        // look for domain
        try {
          CdmiObject domain = domainDaoImpl.findByObjectId(objectId);
          if (domain != null) {
            response.setContentType("application/cdmi-domain+json");
            if (requestedFields == null) {
              return new ResponseEntity<JSONObject>(domain.toJson(), HttpStatus.OK);
            } else {
              String jsonString = getRequestedJson(domain.toJson(), requestedFields).toString();
              return new ResponseEntity<String>(jsonString, HttpStatus.OK);
            }
          }
        } catch (NotFoundException | ClassCastException e3) {
          return new ResponseEntity<String>("Object not found", HttpStatus.NOT_FOUND);
        }
      }
    }

    return new ResponseEntity<String>("Object not found", HttpStatus.NOT_FOUND);
  }

  /**
   * Get path endpoint.
   * 
   * @param request the {@link HttpServletRequest}
   * @param response the {@link HttpServletResponse}
   * @return a JSON serialized {@link Container} or {@link DataObject}
   */
  @RequestMapping(path = "/**", method = RequestMethod.GET,
      consumes = {"application/cdmi-object+json", "application/cdmi-container+json"},
      produces = {"application/cdmi-object+json", "application/cdmi-container+json"})
  public ResponseEntity<?> getCdmiObjectByPath(HttpServletRequest request,
      HttpServletResponse response) {
    String path =
        (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    log.debug("Get path {}", path);
    response.addHeader("X-CDMI-Specification-Version", "1.1.1");

    String[] requestedFields = parseFields(request);
    // look for container
    try {
      CdmiObject container = containerDaoImpl.findByPath(path);
      if (container != null) {
        response.setContentType("application/cdmi-container+json");
        if (requestedFields == null) {
          return new ResponseEntity<JSONObject>(container.toJson(), HttpStatus.OK);
        } else {
          String jsonString = getRequestedJson(container.toJson(), requestedFields).toString();
          return new ResponseEntity<String>(jsonString, HttpStatus.OK);
        }
      }
    } catch (NotFoundException | ClassCastException e1) {
      // look for dataobject
      try {
        DataObject dataObject = dataObjectDaoImpl.findByPath(path);
        if (dataObject != null) {
          response.setContentType("application/cdmi-object+json");
          String range = request.getHeader("Range");
          if (range != null) {
            byte[] content = dataObject.getValue().getBytes();
            String[] ranges = range.split("-");
            try {
              content = Arrays.copyOfRange(content, Integer.valueOf(ranges[0].trim()),
                  Integer.valueOf(ranges[1].trim()));
              dataObject.setValue(new String(content));
            } catch (NumberFormatException numberFormatException) {
              return new ResponseEntity<String>("Bad range", HttpStatus.BAD_REQUEST);
            }
          }
          if (requestedFields == null) {
            return new ResponseEntity<JSONObject>(dataObject.toJson(), HttpStatus.OK);
          } else {
            String jsonString = getRequestedJson(dataObject.toJson(), requestedFields).toString();
            return new ResponseEntity<String>(jsonString, HttpStatus.OK);
          }
        }
      } catch (NotFoundException | ClassCastException e2) {
        return new ResponseEntity<String>("Object not found", HttpStatus.NOT_FOUND);
      }
    }
    return new ResponseEntity<String>("Object not found", HttpStatus.NOT_FOUND);
  }

  /**
   * Put path endpoint.
   * 
   * @param request the {@link HttpServletRequest}
   * @param response the {@link HttpServletResponse}
   * @return a JSON serialized {@link Container} or {@link DataObject}
   */
  @RequestMapping(path = "/**", method = RequestMethod.PUT,
      consumes = {"application/cdmi-object+json", "application/cdmi-container+json",
          "application/cdmi-domain+json"},
      produces = {"application/cdmi-object+json", "application/cdmi-container+json",
          "application/cdmi-domain+json"})
  public ResponseEntity<?> putCdmiObject(@RequestHeader("Content-Type") String contentType,
      @RequestBody String body, HttpServletRequest request, HttpServletResponse response) {
    String path =
        (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    log.debug("Create path {} as {}", path, contentType);
    response.addHeader("X-CDMI-Specification-Version", "1.1.1");

    String[] requestedFields = parseFields(request);
    // create container
    if (contentType.equals("application/cdmi-container")) {
      JSONObject json = new JSONObject(body);
      CdmiObject container = containerDaoImpl.createByPath(path, new Container(json));
      if (container != null) {
        return new ResponseEntity<JSONObject>(container.toJson(), HttpStatus.CREATED);
      }
    }
    // create dataobject
    else if (contentType.equals("application/cdmi-object")) {
      JSONObject json = new JSONObject(body);
      DataObject dataObject = dataObjectDaoImpl.createByPath(path, new DataObject(json));
      if (dataObject != null) {
        return new ResponseEntity<JSONObject>(dataObject.toJson(), HttpStatus.CREATED);
      }
    }
    // create domain
    else if (contentType.equals("application/cdmi-domain")) {
      JSONObject json = new JSONObject(body);
      CdmiObject domain = null;
      if (requestedFields == null) {
        domain = domainDaoImpl.createByPath(path, new Domain(json));
      } else {
        domain = domainDaoImpl.updateByPath(path, new Domain(json), requestedFields);
      }
      if (domain != null) {
        return new ResponseEntity<JSONObject>(domain.toJson(), HttpStatus.CREATED);
      }
    } else {
      return new ResponseEntity<String>("Bad content type", HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<String>("Bad request", HttpStatus.BAD_REQUEST);
  }

  /**
   * Delete path endpoint.
   * 
   * @param request the {@link HttpServletRequest}
   * @param response the {@link HttpServletResponse}
   * @return a {@link ResponseEntity}
   */
  @RequestMapping(path = "/**", method = RequestMethod.DELETE)
  public ResponseEntity<?> deleteCdmiObject(HttpServletRequest request,
      HttpServletResponse response) {
    String path =
        (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    log.debug("Delete path {}", path);
    response.addHeader("X-CDMI-Specification-Version", "1.1.1");

    try {
      dataObjectDaoImpl.deleteByPath(path);
      return new ResponseEntity<String>("Data object deleted", HttpStatus.NO_CONTENT);
    } catch (NotFoundException | ClassCastException e1) {
      try {
        containerDaoImpl.deleteByPath(path);
        return new ResponseEntity<String>("Container deleted", HttpStatus.NO_CONTENT);
      } catch (NotFoundException | ClassCastException e2) {
        try {
          domainDaoImpl.deleteByPath(path);
          return new ResponseEntity<String>("Domain deleted", HttpStatus.NO_CONTENT);
        } catch (NotFoundException | ClassCastException e3) {
          return new ResponseEntity<String>("Not found", HttpStatus.NOT_FOUND);
        }
      }
    }
  }

  private String[] parseFields(HttpServletRequest request) {
    Enumeration<String> attributes = request.getParameterNames();
    String[] requestedFields = null;
    while (attributes.hasMoreElements()) {
      String attributeName = attributes.nextElement();
      requestedFields = attributeName.split(";");
    }
    return requestedFields;
  }

  private JSONObject getRequestedJson(JSONObject object, String[] requestedFields) {
    JSONObject requestedJson = new JSONObject();
    try {
      for (int i = 0; i < requestedFields.length; i++) {
        String field = requestedFields[i];
        if (!field.contains(":")) {
          requestedJson.put(field, object.get(field));
        } else {
          String[] fieldsplit = field.split(":");
          if (object.get(fieldsplit[0]) instanceof JSONObject) {
            JSONObject fieldObject = new JSONObject();
            String prefix = fieldsplit[1];
            String fieldname = fieldsplit[0];
            if (requestedJson.has(fieldname)) {
              fieldObject = requestedJson.getJSONObject(fieldname);
            }
            Iterator<?> keys = object.getJSONObject(fieldname).keys();
            while (keys.hasNext()) {
              String key = (String) keys.next();
              if (key.startsWith(prefix)) {
                fieldObject.put(key, object.getJSONObject(fieldname).get(key));
              }
            }
            if (fieldObject.length() != 0) {
              requestedJson.put(fieldname, fieldObject);
            }
          } else if (field.startsWith("children:")) {
            String range = field.split("children:")[1];
            String[] rangeSplit = range.split("-");
            List<String> requestedChildren = new ArrayList<String>();
            JSONArray children = object.getJSONArray("children");
            int startIndex = Integer.valueOf(rangeSplit[0]);
            if (rangeSplit.length > 1) {
              int endIndex = Integer.valueOf(rangeSplit[1]);
              for (int j = startIndex; j <= endIndex; j++)
                requestedChildren.add(children.getString(j));
            } else {
              requestedChildren.add(children.getString(startIndex));
            }
            requestedJson.put("children", requestedChildren);
          } else if (field.startsWith("value:")) {
            String range = field.split("value:")[1];
            String[] rangeSplit = range.split("-");
            requestedJson
                .put("value",
                    new String(Arrays.copyOfRange(object.getString("value").getBytes(),
                        Integer.valueOf(rangeSplit[0].trim()),
                        Integer.valueOf(rangeSplit[1].trim()))));
          } else
            throw new BadRequestException("Bad prefix");

        }
      }
      if (requestedJson.has("childrenrange") && requestedJson.has("children")) {
        requestedJson.put("childrenrange",
            "0-" + String.valueOf(requestedJson.getJSONArray("children").length() - 1));
      }
    } catch (JSONException e) {
      throw new BadRequestException("bad field");
    } catch (IndexOutOfBoundsException | NumberFormatException e) {
      throw new BadRequestException("bad range");
    }
    return requestedJson;
  }

}
