var coll = document.getElementsByClassName("mainInfo");
var i;

if (coll.length === 1) {
    coll[0].parentElement.classList.add("active")
    coll[0].classList.add("static")
} else {
    for (i = 0; i < coll.length; i++) {
        coll[i].addEventListener("click", function () {
            this.parentElement.classList.toggle("active");
        });
        coll[i].classList.add("clickable")
    }
}