//
//  AuthViewModel.swift
//  ios-app
//
//  Created by Naufal Fawwaz Andriawan on 10/09/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
import shared
import GoogleSignIn
import UIKit
import Combine

class AuthViewModel: ObservableObject {
    @Published var isSuccessLogin = false
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var currentUser: User?
    
    private let loginIdTokenUseCase: LoginIdTokenUseCase
    private let getUserUseCase: GetUserUseCase
    private var cancellable = Set<AnyCancellable>()
    
    init() {
        let koinHelper = KoinHelper()
        self.loginIdTokenUseCase = koinHelper.getLoginIdTokenUseCase()
        self.getUserUseCase = koinHelper.getGetUserUseCase()
    }
    
    func login(_ completion: @escaping (String) -> Void) {
        guard let root = (UIApplication.shared.connectedScenes.first as? UIWindowScene)?
            .windows
            .first(where: { $0.isKeyWindow })?
            .rootViewController else { return }
        
        DispatchQueue.main.async {
            self.isLoading = true
            self.errorMessage = nil
        }
        
        GIDSignIn.sharedInstance.signIn(withPresenting: root) { result, error in
            if let error = error {
                DispatchQueue.main.async {
                    self.isLoading = false
                    self.errorMessage = error.localizedDescription
                }
                return
            }
            
            guard let idToken = result?.user.idToken?.tokenString else {
                DispatchQueue.main.async {
                    self.isLoading = false
                    self.errorMessage = "Failed to get ID token."
                }
                return
            }
            
            completion(idToken)
        }
    }
    
    func handleIdToken(idToken: String) {
        let loginIdToken = self.loginIdTokenUseCase.execute(idTokenParam: IdTokenParam(idToken: idToken)).toPublisher()
        loginIdToken
            .receive(on: DispatchQueue.main)
            .sink { [weak self] result in
                switch onEnum(of: result) {
                case .loading:
                    self?.isLoading = true
                    self?.errorMessage = ""
                case .success:
                    self?.isLoading = false
                    self?.isSuccessLogin = true
                    self?.errorMessage = ""
                case .error(let error):
                    self?.isLoading = false
                    self?.errorMessage = error.exception.message
                }
            }.store(in: &cancellable)
    }
}

