(function() {
    var app = angular.module('athena');

    app.factory('FilePost', function ($resource) {
        return $resource('/services/pdf/generate',null,{
            create: {
                method: "POST",
                transformRequest: angular.identity,
                headers: { 'Content-Type': undefined }
            }
        });
    });
})();