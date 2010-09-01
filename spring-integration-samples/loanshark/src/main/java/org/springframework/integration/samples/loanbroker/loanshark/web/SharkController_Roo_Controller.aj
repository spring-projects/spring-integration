package org.springframework.integration.samples.loanbroker.loanshark.web;


import java.lang.Long;
import java.lang.String;
import javax.validation.Valid;

import org.springframework.integration.samples.loanbroker.loanshark.domain.LoanShark;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

privileged aspect SharkController_Roo_Controller {
    
    @RequestMapping(value = "/loanshark", method = RequestMethod.POST)
    public String SharkController.create(@Valid LoanShark loanShark, BindingResult result, ModelMap modelMap) {
        if (loanShark == null) throw new IllegalArgumentException("A loanShark is required");
        if (result.hasErrors()) {
            modelMap.addAttribute("loanShark", loanShark);
            return "loanshark/create";
        }
        loanShark.persist();
        return "redirect:/loanshark/" + loanShark.getId();
    }
    
    @RequestMapping(value = "/loanshark/form", method = RequestMethod.GET)
    public String SharkController.createForm(ModelMap modelMap) {
        modelMap.addAttribute("loanShark", new LoanShark());
        return "loanshark/create";
    }
    
    @RequestMapping(value = "/loanshark/{id}", method = RequestMethod.GET)
    public String SharkController.show(@PathVariable("id") Long id, ModelMap modelMap) {
        if (id == null) throw new IllegalArgumentException("An Identifier is required");
        modelMap.addAttribute("loanShark", LoanShark.findLoanShark(id));
        return "loanshark/show";
    }
    
    @RequestMapping(value = "/loanshark", method = RequestMethod.GET)
    public String SharkController.list(@RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size, ModelMap modelMap) {
        if (page != null || size != null) {
            int sizeNo = size == null ? 10 : size.intValue();
            modelMap.addAttribute("loansharks", LoanShark.findLoanSharkEntries(page == null ? 0 : (page.intValue() - 1) * sizeNo, sizeNo));
            float nrOfPages = (float) LoanShark.countLoanSharks() / sizeNo;
            modelMap.addAttribute("maxPages", (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages));
        } else {
            modelMap.addAttribute("loansharks", LoanShark.findAllLoanSharks());
        }
        return "loanshark/list";
    }
    
    @RequestMapping(method = RequestMethod.PUT)
    public String SharkController.update(@Valid LoanShark loanShark, BindingResult result, ModelMap modelMap) {
        if (loanShark == null) throw new IllegalArgumentException("A loanShark is required");
        if (result.hasErrors()) {
            modelMap.addAttribute("loanShark", loanShark);
            return "loanshark/update";
        }
        loanShark.merge();
        return "redirect:/loanshark/" + loanShark.getId();
    }
    
    @RequestMapping(value = "/loanshark/{id}/form", method = RequestMethod.GET)
    public String SharkController.updateForm(@PathVariable("id") Long id, ModelMap modelMap) {
        if (id == null) throw new IllegalArgumentException("An Identifier is required");
        modelMap.addAttribute("loanShark", LoanShark.findLoanShark(id));
        return "loanshark/update";
    }
    
    @RequestMapping(value = "/loanshark/{id}", method = RequestMethod.DELETE)
    public String SharkController.delete(@PathVariable("id") Long id, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "size", required = false) Integer size) {
        if (id == null) throw new IllegalArgumentException("An Identifier is required");
        LoanShark.findLoanShark(id).remove();
        return "redirect:/loanshark?page=" + ((page == null) ? "1" : page.toString()) + "&size=" + ((size == null) ? "10" : size.toString());
    }
    
    @RequestMapping(value = "find/ByName/form", method = RequestMethod.GET)
    public String SharkController.findLoanSharksByNameForm(ModelMap modelMap) {
        return "loanshark/findLoanSharksByName";
    }
    
    @RequestMapping(value = "find/ByName", method = RequestMethod.GET)
    public String SharkController.findLoanSharksByName(@RequestParam("name") String name, ModelMap modelMap) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException("A Name is required.");
        modelMap.addAttribute("loansharks", LoanShark.findLoanSharksByName(name).getResultList());
        return "loanshark/list";
    }
    
}
