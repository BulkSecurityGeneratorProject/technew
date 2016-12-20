(function() {
    'use strict';

    angular
        .module('technewApp')
        .config(stateConfig);

    stateConfig.$inject = ['$stateProvider'];

    function stateConfig($stateProvider) {
        $stateProvider
        .state('author', {
            parent: 'entity',
            url: '/author',
            data: {
                authorities: ['ROLE_ADMIN'],
                pageTitle: 'technewApp.author.home.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/author/authors.html',
                    controller: 'AuthorController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('author');
                    $translatePartialLoader.addPart('post');
                    $translatePartialLoader.addPart('status');
                    $translatePartialLoader.addPart('global');
                    return $translate.refresh();
                }]
            }
        })
        .state('author-detail', {
            parent: 'entity',
            url: '/author/{id}',
            data: {
                authorities: [],
                pageTitle: 'technewApp.author.detail.title'
            },
            views: {
                'content@': {
                    templateUrl: 'app/entities/author/author-detail.html',
                    controller: 'AuthorDetailController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                    $translatePartialLoader.addPart('author');
                    $translatePartialLoader.addPart('status');
                    return $translate.refresh();
                }],
                entity: ['$stateParams', 'Author', function($stateParams, Author) {
                    return Author.get({id : $stateParams.id}).$promise;
                }]
            }
        })
        .state('author.new', {
            parent: 'author',
            url: '/new',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/author/author-dialog.html',
                    controller: 'AuthorDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'md',
                    resolve: {
                        entity: function () {
                            return {
                                name: null,
                                birthDay: null,
                                address: null,
                                phone: null,
                                avatar: null,
                                status: null,
                                aboutMe: null,
                                id: null
                            };
                        }
                    }
                }).result.then(function() {
                    $state.go('author', null, { reload: 'author' });
                }, function() {
                    $state.go('author');
                });
            }]
        })
        .state('author.edit', {
            parent: 'author',
            url: '/{id}/edit',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/author/author-dialog.html',
                    controller: 'AuthorDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'md',
                    resolve: {
                        entity: ['Author', function(Author) {
                            return Author.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('author', null, { reload: 'author' });
                }, function() {
                    $state.go('^');
                });
            }]
        })
        .state('author.delete', {
            parent: 'author',
            url: '/{id}/delete',
            data: {
                authorities: ['ROLE_ADMIN']
            },
            onEnter: ['$stateParams', '$state', '$uibModal', function($stateParams, $state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/entities/author/author-delete-dialog.html',
                    controller: 'AuthorDeleteController',
                    controllerAs: 'vm',
                    size: 'md',
                    resolve: {
                        entity: ['Author', function(Author) {
                            return Author.get({id : $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function() {
                    $state.go('author', null, { reload: 'author' });
                }, function() {
                    $state.go('^');
                });
            }]
        });
    }

})();
