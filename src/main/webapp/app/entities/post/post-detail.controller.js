(function() {
    'use strict';

    angular
        .module('technewApp')
        .controller('PostDetailController', PostDetailController);

    PostDetailController.$inject = ['$scope', '$rootScope', '$stateParams', 'previousState', 'entity', 'Post', 'Comment', 'Tag', 'Author', 'Category'];

    function PostDetailController($scope, $rootScope, $stateParams, previousState, entity, Post, Comment, Tag, Author, Category) {
        var vm = this;

        vm.post = entity;
        vm.previousState = previousState.name;

        var unsubscribe = $rootScope.$on('technewApp:postUpdate', function(event, result) {
            vm.post = result;
        });
        $scope.$on('$destroy', unsubscribe);
    }
})();
