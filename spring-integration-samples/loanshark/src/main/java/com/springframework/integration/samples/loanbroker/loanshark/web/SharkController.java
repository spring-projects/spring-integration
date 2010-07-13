package com.springframework.integration.samples.loanbroker.loanshark.web;

import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;

import com.springframework.integration.samples.loanbroker.loanshark.domain.LoanShark;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

@RooWebScaffold(path = "loanshark", automaticallyMaintainView = true, formBackingObject = LoanShark.class)
@RequestMapping("/loanshark/**")
@Controller
public class SharkController {
}
