/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.vet.Vets;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 */
@Controller
class VisitController {

    private final VisitRepository visits;
    private final PetRepository pets;
    private final VetRepository vets;
    private final OwnerRepository owners;

    public VisitController(VisitRepository visits, PetRepository pets, VetRepository vets, OwnerRepository owners) {
        this.visits = visits;
        this.pets = pets;
        this.vets = vets;
        this.owners = owners;
    }

    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }

    /**
     * Called before each and every @RequestMapping annotated method.
     * 2 goals:
     * - Make sure we always have fresh data
     * - Since we do not use the session scope, make sure that Pet object always has an id
     * (Even though id is not part of the form fields)
     *
     * @param petId
     * @return Pet
     */
    @ModelAttribute("visit")
    public Visit loadPetWithVisit(@PathVariable("petId") int petId, Map<String, Object> model) {
        Pet pet = this.pets.findById(petId);
        model.put("pet", pet);
        Vets vets = new Vets();
        vets.getVetList().addAll(this.vets.findAll());
        model.put("vets", vets);
        Visit visit = new Visit();
        pet.addVisit(visit);
        return visit;
    }

    // Spring MVC calls method loadPetWithVisit(...) before initNewVisitForm is called
    @GetMapping("/owners/*/pets/{petId}/visits/new")
    public String initNewVisitForm(@PathVariable("petId") int petId, Map<String, Object> model) {
        Vets vets = new Vets();
        vets.getVetList().addAll(this.vets.findAll());
        model.put("vets", vets);
        return "pets/createOrUpdateVisitForm";
    }

    // Spring MVC calls method loadPetWithVisit(...) before processNewVisitForm is called
    @PostMapping("/owners/{ownerId}/pets/{petId}/visits/new")
    public String processNewVisitForm(@Valid Visit visit, BindingResult result, Map<String, Object> model) {
        if (result.hasErrors()) {
            Vets vets = new Vets();
            vets.getVetList().addAll(this.vets.findAll());
            model.put("vets", vets);
            return "pets/createOrUpdateVisitForm";
        } else {
            visit.setActual(1);
            this.visits.save(visit);
            return "redirect:/owners/{ownerId}";
        }
    }

    @GetMapping("/owners/{ownerId}/pets/{petId}/visits/edit/{id}")
    public String editVisit(@PathVariable("id") int id,@PathVariable("ownerId") int ownerId, Model model) {
        Visit visit = visits.findById(id);
        List<Pet> pets = owners.findById(ownerId).getPets();
        model.addAttribute("pets", pets);
        model.addAttribute(visit);
        return "pets/UpdateVisitForm";
    }

    // Spring MVC calls method loadPetWithVisit(...) before processNewVisitForm is called

    @PostMapping("/owners/{ownerId}/pets/{petId}/visits/edit/{id}")
    public String processEditVisit(@Valid Visit visit, BindingResult result, Map<String, Object> model, @PathVariable(name = "id") int id, @PathVariable(name = "ownerId") int ownerId) {
        if (result.hasErrors()) {
            Visit visit1 = visits.findById(id);
            model.put("visit", visit1);
            return "pets/UpdateVisitForm";
        } else {
            Visit visitOld = visits.findById(id);
            visit.setId(id);
            visit.setActual(visitOld.getActual());
            this.visits.save(visit);
            return "redirect:/owners/{ownerId}";
        }
    }

    @GetMapping("/owners/{ownerId}/pets/{petId}/visits/delete/{id}")
    public String cancelVisit(@PathVariable("id") int id) {
        Visit visit = visits.findById(id);
        if (visit.getActual() == 1) visit.setActual(0);
        else visit.setActual(1);
        visits.save(visit);
        return "redirect:/owners/{ownerId}";
    }

}
