package com.example.enrique.firebasedemo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private SignInButton mGoogleBtn;
    private Button mLoginButton;
    private Button mResetPassword;
    private EditText editTextEmail;
    private EditText editTextPassword;

    private Boolean flagAuth = false;

    private static final int RC_SIGN_IN = 1;

    private GoogleApiClient mGoogleApiClient;

    private FirebaseAuth mAuth;

    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mGoogleBtn = (SignInButton) findViewById(R.id.googleBtn);
        mLoginButton = (Button) findViewById(R.id.btnLogin);
        mResetPassword = (Button)findViewById(R.id.btnResetPassword);
        editTextEmail = (EditText)findViewById(R.id.editTextEmail);
        editTextPassword = (EditText)findViewById(R.id.editTextPassword);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser() !=  null && flagAuth == false){
                    flagAuth = true;

                    // Inserta el usuario en la base de datos
                    final FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference firebaseRoot = database.getReference();
                    firebaseRoot.child("users").child(firebaseAuth.getCurrentUser().getUid()).setValue(new User(firebaseAuth.getCurrentUser().getEmail(), firebaseAuth.getCurrentUser().getDisplayName()));

                    editTextEmail.setText("");
                    editTextPassword.setText("");
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));

                    Toast.makeText(LoginActivity.this, "Login Correcto!!!", Toast.LENGTH_LONG).show();
                }
            }
        };

        // Configure google sign in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(LoginActivity.this, "Error Google Login", Toast.LENGTH_LONG).show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mGoogleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SignInGoogle();

            }
        });

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(editTextEmail.getText().toString() != null
                        && !editTextEmail.getText().toString().equals("")
                        && editTextPassword.getText().toString() != null
                        && !editTextPassword.getText().toString().equals("")) {

                    CreateOrSignIn(editTextEmail.getText().toString(), editTextPassword.getText().toString());
                }
            }
        });

        mResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(editTextEmail.getText().toString() != null && !editTextEmail.getText().toString().equals("")) {

                    ResetPassword(editTextEmail.getText().toString());
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        flagAuth = false;
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void CreateOrSignIn(String email, String password){
        final String mEmail = email;
        final String mPassword = password;

        mAuth.createUserWithEmailAndPassword(mEmail, mPassword)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        String excepcion = "";
                        if(task != null && task.getException() != null) {
                            excepcion = task.getException().getMessage();
                        }

                        if (!task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Creación de usuario Incorrecta", Toast.LENGTH_SHORT).show();

                            if(excepcion.equals("The email address is already in use by another account.")){
                                // Intentamos hacer Login
                                SignIn(mEmail, mPassword);
                            }
                        }

                    }
                });
    }

    private void SignIn(String email, String password){
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (!task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Login Incorrecto", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
    }

    private void ResetPassword(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Email de recuperación enviado", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void SignInGoogle(){
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN){
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if(result.isSuccess()){
                // Si el Login con Google va bien se hacce Login con Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            }
            else{
                // Falla el Login de Google
                // TODO...
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(LoginActivity.this, "Autenticación fallida", Toast.LENGTH_LONG).show();
                        }
                    }
                });

    }
}
