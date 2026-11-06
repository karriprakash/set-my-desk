package com.setmydesk.desk;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/desk")
public class DeskController {

    private final DeskLayoutSolver solver;

    public DeskController(DeskLayoutSolver solver) {
        this.solver = solver;
    }

    @GetMapping("/catalog")
    public List<DeskModels.CatalogItem> catalog() {
        return Arrays.stream(DeskItemType.values())
                .map(t -> new DeskModels.CatalogItem(t, t.label, t.widthCm, t.depthCm, t.heightCm, t.color))
                .toList();
    }

    @PostMapping("/layout")
    public DeskModels.Layout layout(@Valid @RequestBody DeskModels.LayoutRequest request) {
        return solver.solve(request);
    }
}
