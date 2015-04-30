define(['./module1', './module3'], function (m1, m3) {
    console.log('loaded module2 into context: ' + this);

    return {
        func2: function () {
            console.log("invoked func2 in module2!");
            m1.func11();
        }
    };
});