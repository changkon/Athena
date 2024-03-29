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

     app.factory('StoreQuestionPost', function ($resource) {
            return $resource('/services/question/store',null,{
                create: {
                    method: "POST",
                    headers: { 'Content-Type': "application/json" }
                }
            });
        });

     app.factory('InsertGroupPost', function ($resource) {
            return $resource('/services/group/create',null,{
                create: {
                    method: "POST",
                    headers: { 'Content-Type': "application/json" }
                }
            });
        });

     app.factory('RateQuestionPost', function ($resource) {
            return $resource('/services/question/rate',null,{
                create: {
                    method: "POST",
                    headers: { 'Content-Type': "application/json" }
                }
            });
        });

     app.factory('GetCategories', function ($resource) {
            return $resource('/services/question/categories',null,{
                create: {
                    method: "GET",
                    headers: { 'Content-Type': "application/json" }
                }
            });
        });

     app.factory('GetGroups', function ($resource) {
            return $resource('/services/group/:id',null,{
                create: {
                    method: "GET",
                    headers: { 'Content-Type': "application/json" }
                }
            });
        });

        app.factory('GetAccountName', function ($resource) {
            return $resource('/services/account/:id',null,{
                create: {
                    method: "GET",
                    headers: { 'Content-Type': "application/json" }
                }
            });
        });

        app.factory('GetQuestions', function ($resource) {
            return $resource('/services/question/questions',null,{
                create: {
                    method: "POST",
                    headers: { 'Content-Type': "application/json" }
                }
            });
        });


})();