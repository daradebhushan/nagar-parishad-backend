
try {
    var dept = departmentRepository.findById(5L).orElse(null);
    if (dept) {
        println("Dept 5 Found: " + dept.getName());
    } else {
        println("Dept 5 NOT Found");
        // Create it if missing?
        var newDept = new com.nagar.parishad.backend.entity.Department();
        newDept.setId(5L);
        newDept.setName("Ghantagadi");
        newDept.setNameMr("घंटागाडी");
        departmentRepository.save(newDept);
        println("Dept 5 Created: Ghantagadi");
    }
} catch (e) {
    e.printStackTrace();
}
